package dev.iamrat.study.application;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.core.study.StudyAssistant;
import dev.iamrat.study.domain.EvidenceVerifier;
import dev.iamrat.study.domain.GapFinder;
import dev.iamrat.study.domain.KeyPointExtractor;
import dev.iamrat.study.domain.StudyQuestion;
import dev.iamrat.study.domain.StudyQuestion.Origin;
import dev.iamrat.study.domain.StudyQuestionRepository;
import dev.iamrat.study.domain.StudyRecord;
import dev.iamrat.study.domain.StudyRecordRepository;
import dev.iamrat.study.domain.StudySource;
import dev.iamrat.study.domain.StudySourceRepository;
import dev.iamrat.study.support.error.StudyErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * study에서 LLM을 부르는 경로는 모두 여기에 있다. 사용자가 버튼을 눌렀을 때만 호출된다.
 * 매일 반복 루프(StudyPracticeService)는 StudyAssistant를 갖지 않으므로 LLM을 부를 수 없다.
 * LLM을 기다리는 동안 트랜잭션을 열어 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StudyAiService {

    private static final int STUDENT_QUESTION_LIMIT = 3;
    // study_records.result 컬럼 길이
    private static final int RESULT_MAX = 2000;
    private static final int CONTEXT_RADIUS = 1500;
    // 규칙 꼬리질문에 옮겨 적는 근거 길이. 문제 칸(500자)에 들어가게 줄인다.
    private static final int EXAMPLE_QUOTE_MAX = 100;

    public record FollowUp(Long id, String question, String evidence, String origin) {
    }

    private final StudySourceRepository sourceRepository;
    private final StudyQuestionRepository questionRepository;
    private final StudyRecordRepository recordRepository;
    private final TransactionTemplate transactionTemplate;
    private final StudyAssistant studyAssistant;
    private final Clock clock;

    public List<String> teach(Long ownerAccountId, Long sourceId, String explanation) {
        StudySource source = sourceRepository.getOwned(sourceId, ownerAccountId);
        List<String> asked = studyAssistant
            .askAsStudent(source.getContent(), explanation, STUDENT_QUESTION_LIMIT).stream()
            .filter(question -> question != null && !question.isBlank())
            .limit(STUDENT_QUESTION_LIMIT)
            .toList();
        List<String> questions = asked.isEmpty() ? gapQuestions(source.getContent(), explanation) : asked;
        recordRepository.save(StudyRecord.teaching(source, explanation, cut(String.join("\n", questions), RESULT_MAX), now()));
        return questions;
    }

    /**
     * 앞 문제를 한 단계 더 파고드는 문제를 하나 만들어 바로 복습 목록에 올린다. LLM은 한 번만 부른다.
     * 근거가 자료에 그대로 없으면 버리고, 앞 문제의 근거로 예를 묻는 규칙 문제로 대신한다.
     * 사용자의 답은 보내지 않는다. LLM이 채점할 거리를 주지 않는다.
     */
    public FollowUp followUp(Long ownerAccountId, Long questionId) {
        StudyQuestion parent = questionRepository.findByIdAndOwnerAccountId(questionId, ownerAccountId)
            .orElseThrow(() -> new CustomException(StudyErrorCode.QUESTION_NOT_FOUND));
        StudySource source = sourceRepository.getOwned(parent.getSourceId(), ownerAccountId);
        String content = source.getContent();
        QuestionDraft drafted = studyAssistant
            .draftFollowUps(around(content, parent.getEvidence()), parent.getQuestion(), parent.getEvidence(), 1).stream()
            .filter(draft -> StudySourceService.isUsable(content, draft))
            .findFirst()
            .orElse(null);
        Origin origin = drafted == null ? Origin.RULE : Origin.LLM;
        QuestionDraft chosen = drafted != null ? drafted : exampleQuestion(parent.getEvidence());
        LocalDateTime now = now();
        StudyQuestion created = transactionTemplate.execute(status -> {
            StudyQuestion saved = questionRepository.save(
                StudyQuestion.create(source, chosen.question(), chosen.evidence(), origin, now));
            recordRepository.save(StudyRecord.followUp(source, saved, parent, now));
            return saved;
        });
        return new FollowUp(created.getId(), created.getQuestion(), created.getEvidence(), origin.name());
    }

    /** 근거가 자료 뒤쪽에 있어도 문맥이 되도록 근거 주변만 자른다. 앞 4000자만 보내는 문제 생성과 다르다. */
    static String around(String content, String evidence) {
        String text = EvidenceVerifier.normalize(content);
        int at = text.indexOf(EvidenceVerifier.normalize(evidence));
        if (at < 0) {
            return text.substring(0, Math.min(text.length(), CONTEXT_RADIUS * 2));
        }
        int from = Math.max(0, at - CONTEXT_RADIUS);
        int to = Math.min(text.length(), at + EvidenceVerifier.normalize(evidence).length() + CONTEXT_RADIUS);
        return text.substring(from, to);
    }

    private static QuestionDraft exampleQuestion(String evidence) {
        String quote = evidence.length() <= EXAMPLE_QUOTE_MAX ? evidence : evidence.substring(0, EXAMPLE_QUOTE_MAX) + "…";
        return new QuestionDraft("'" + quote + "' 부분을 예를 들어 설명해 보세요.", evidence);
    }

    private static List<String> gapQuestions(String content, String explanation) {
        List<String> gaps = GapFinder.missing(KeyPointExtractor.extract(content), explanation);
        if (gaps.isEmpty()) {
            return List.of("처음 듣는 사람에게 예시를 하나 들어 줄 수 있나요?");
        }
        return gaps.stream()
            .limit(STUDENT_QUESTION_LIMIT)
            .map(gap -> "'" + gap + "' 부분은 설명에 안 나왔어요. 어떤 뜻인가요?")
            .toList();
    }

    private static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(max - 1)) ? max - 1 : max;
        return text.substring(0, end);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
