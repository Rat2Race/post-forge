package dev.iamrat.study.application;

import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.core.study.StudyAssistant;
import dev.iamrat.study.domain.EvidenceVerifier;
import dev.iamrat.study.domain.KeyPointExtractor;
import dev.iamrat.study.domain.StudyQuestion;
import dev.iamrat.study.domain.StudyQuestion.Origin;
import dev.iamrat.study.domain.StudyQuestionRepository;
import dev.iamrat.study.domain.StudyRecord;
import dev.iamrat.study.domain.StudyRecordRepository;
import dev.iamrat.study.domain.StudySource;
import dev.iamrat.study.domain.StudySourceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class StudySourceService {

    private static final int QUESTION_LIMIT = 8;

    public record SourceSummary(Long id, String title, String questionStatus, LocalDateTime createdAt) {
    }

    public record SourceDetail(
        Long id,
        String title,
        String content,
        String questionStatus,
        int draftedQuestionCount,
        int discardedQuestionCount,
        List<String> keyPoints,
        List<QuestionView> questions,
        String emptyReason,
        LocalDateTime createdAt
    ) {
    }

    public record QuestionView(Long id, String question, String evidence, String origin, int box, LocalDateTime dueAt) {
    }

    private final StudySourceRepository sourceRepository;
    private final StudyQuestionRepository questionRepository;
    private final StudyRecordRepository recordRepository;
    private final StudyAssistant studyAssistant;
    private final TransactionTemplate transactionTemplate;
    private final Executor executor;
    private final Clock clock;

    public StudySourceService(
        StudySourceRepository sourceRepository,
        StudyQuestionRepository questionRepository,
        StudyRecordRepository recordRepository,
        StudyAssistant studyAssistant,
        TransactionTemplate transactionTemplate,
        @Qualifier("applicationTaskExecutor") Executor executor,
        Clock clock
    ) {
        this.sourceRepository = sourceRepository;
        this.questionRepository = questionRepository;
        this.recordRepository = recordRepository;
        this.studyAssistant = studyAssistant;
        this.transactionTemplate = transactionTemplate;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * 자료 저장은 바로 커밋하고, 느린 LLM 질문 생성은 뒤로 넘긴다.
     * 이 메서드에 트랜잭션을 걸면 생성 작업이 커밋 전 데이터를 못 볼 수 있다.
     * ponytail: 메모리 실행기라 재시작하면 진행 중이던 생성이 사라지고 GENERATING에 머문다.
     * 잃으면 안 될 때 DB 작업 큐(SKIP LOCKED 선점 + 재시도)로 옮긴다.
     */
    public Long create(Long ownerAccountId, String title, String content) {
        StudySource source = sourceRepository.save(StudySource.create(ownerAccountId, title, content, now()));
        executor.execute(() -> {
            try {
                generateQuestions(source);
            } catch (RuntimeException e) {
                log.error("study question generation failed. sourceId={}", source.getId(), e);
            }
        });
        return source.getId();
    }

    @Transactional(readOnly = true)
    public List<SourceSummary> list(Long ownerAccountId) {
        return sourceRepository.findByOwnerAccountIdOrderByIdDesc(ownerAccountId).stream()
            .map(source -> new SourceSummary(
                source.getId(), source.getTitle(), source.getQuestionStatus().name(), source.getCreatedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public SourceDetail get(Long ownerAccountId, Long sourceId) {
        StudySource source = sourceRepository.getOwned(sourceId, ownerAccountId);
        List<QuestionView> questions = questionRepository.findBySourceIdOrderById(sourceId).stream()
            .map(question -> new QuestionView(question.getId(), question.getQuestion(), question.getEvidence(),
                question.getOrigin().name(), question.getBox(), question.getDueAt()))
            .toList();
        return new SourceDetail(source.getId(), source.getTitle(), source.getContent(),
            source.getQuestionStatus().name(), source.getDraftedQuestionCount(), source.getDiscardedQuestionCount(),
            KeyPointExtractor.extract(source.getContent()), questions, emptyReason(source, questions), source.getCreatedAt());
    }

    @Transactional
    public Long addQuestion(Long ownerAccountId, Long sourceId, String question, String evidence) {
        StudySource source = sourceRepository.getOwned(sourceId, ownerAccountId);
        StudyQuestion created = questionRepository.save(
            StudyQuestion.create(source, question, evidence, Origin.USER, now()));
        recordRepository.save(StudyRecord.questionMade(source, created, now()));
        return created.getId();
    }

    /** LLM 호출은 트랜잭션 밖에서 하고, 결과 저장만 짧은 트랜잭션으로 묶는다. */
    private void generateQuestions(StudySource source) {
        String content = source.getContent();
        List<QuestionDraft> drafts = studyAssistant.draftQuestions(content, QUESTION_LIMIT);
        List<QuestionDraft> verified = drafts.stream().filter(draft -> isUsable(content, draft)).toList();
        // 버린 수는 근거 실패만 센다. 근거는 맞는데 문제가 길어 못 쓴 초안까지 세면 근거 검증 통과율(ADR-008 게이트)이 낮게 잡힌다.
        int discarded = (int) drafts.stream().filter(draft -> !EvidenceVerifier.isQuoted(content, draft.evidence())).count();
        Origin origin = verified.isEmpty() ? Origin.RULE : Origin.LLM;
        List<QuestionDraft> chosen = verified.isEmpty()
            ? RuleQuestionGenerator.generate(content, QUESTION_LIMIT)
            : verified.stream().limit(QUESTION_LIMIT).toList();
        LocalDateTime now = now();

        transactionTemplate.executeWithoutResult(status -> {
            chosen.forEach(draft -> questionRepository.save(
                StudyQuestion.create(source, draft.question(), draft.evidence(), origin, now)));
            sourceRepository.findById(source.getId()).orElseThrow().questionsReady(drafts.size(), discarded);
        });
    }

    /** 문제가 0개면 왜 그런지, 자료를 어떻게 고치면 되는지 알려 준다. */
    private static String emptyReason(StudySource source, List<QuestionView> questions) {
        if (source.getQuestionStatus() != StudySource.QuestionStatus.READY || !questions.isEmpty()) {
            return null;
        }
        String fix = "근거로 쓸 8자 이상의 문장이나 마크다운 제목(#)·목록(-)을 찾지 못했어요. 내용을 더 붙이거나 제목·목록으로 나눠 주세요.";
        int drafted = source.getDraftedQuestionCount();
        if (drafted > 0 && source.getDiscardedQuestionCount() == drafted) {
            return "AI가 낸 문제 " + drafted + "개는 근거가 자료에 그대로 없어 모두 버렸고, " + fix;
        }
        return fix;
    }

    // 길이 상한은 study_questions 컬럼 길이와 같다.
    static boolean isUsable(String content, QuestionDraft draft) {
        return draft.question() != null && !draft.question().isBlank() && draft.question().length() <= 500
            && draft.evidence() != null && draft.evidence().length() <= 1000
            && EvidenceVerifier.isQuoted(content, draft.evidence());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
