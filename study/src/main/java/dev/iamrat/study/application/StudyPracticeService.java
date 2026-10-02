package dev.iamrat.study.application;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.study.StudyAssistant;
import dev.iamrat.study.domain.GapFinder;
import dev.iamrat.study.domain.KeyPointExtractor;
import dev.iamrat.study.domain.ReviewGrade;
import dev.iamrat.study.domain.StudyQuestion;
import dev.iamrat.study.domain.StudyQuestionRepository;
import dev.iamrat.study.domain.StudyRecord;
import dev.iamrat.study.domain.StudyRecordRepository;
import dev.iamrat.study.domain.StudySource;
import dev.iamrat.study.domain.StudySourceRepository;
import dev.iamrat.study.support.error.StudyErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyPracticeService {

    private static final int STUDENT_QUESTION_LIMIT = 3;
    // study_records.result 컬럼 길이
    private static final int RESULT_MAX = 2000;

    public record DueQuestion(Long id, Long sourceId, String sourceTitle, String question, String evidence, int box) {
    }

    public record ReviewResult(int box, LocalDateTime dueAt) {
    }

    public record RecallResult(int recalled, int total, List<String> missed) {
    }

    public record RecordView(
        Long id,
        String kind,
        Long sourceId,
        String sourceTitle,
        String prompt,
        String userText,
        String result,
        LocalDateTime createdAt
    ) {
    }

    private final StudySourceRepository sourceRepository;
    private final StudyQuestionRepository questionRepository;
    private final StudyRecordRepository recordRepository;
    private final StudyAssistant studyAssistant;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DueQuestion> today(Long ownerAccountId) {
        List<StudyQuestion> due = questionRepository
            .findTop20ByOwnerAccountIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(ownerAccountId, now());
        Map<Long, String> titles = sourceRepository
            .findAllById(due.stream().map(StudyQuestion::getSourceId).distinct().toList()).stream()
            .collect(Collectors.toMap(StudySource::getId, StudySource::getTitle));
        return due.stream()
            .map(question -> new DueQuestion(question.getId(), question.getSourceId(),
                titles.get(question.getSourceId()), question.getQuestion(), question.getEvidence(), question.getBox()))
            .toList();
    }

    @Transactional
    public ReviewResult review(Long ownerAccountId, Long questionId, String answer, ReviewGrade grade) {
        StudyQuestion question = questionRepository.findByIdAndOwnerAccountId(questionId, ownerAccountId)
            .orElseThrow(() -> new CustomException(StudyErrorCode.QUESTION_NOT_FOUND));
        LocalDateTime now = now();
        question.review(grade, now);
        StudySource source = sourceRepository.getOwned(question.getSourceId(), ownerAccountId);
        recordRepository.save(StudyRecord.answer(source, question, answer, grade, now));
        return new ReviewResult(question.getBox(), question.getDueAt());
    }

    /** 빈 페이지에 쓴 뒤 자료와 대조해 사용자가 직접 체크한 핵심 항목으로 기록한다. 채점하지 않는다. */
    @Transactional
    public RecallResult recall(Long ownerAccountId, Long sourceId, String text, List<Integer> recalledIndexes) {
        StudySource source = sourceRepository.getOwned(sourceId, ownerAccountId);
        List<String> keyPoints = KeyPointExtractor.extract(source.getContent());
        Set<Integer> recalled = new HashSet<>(recalledIndexes);
        if (recalled.stream().anyMatch(index -> index == null || index < 0 || index >= keyPoints.size())) {
            throw new CustomException(StudyErrorCode.INVALID_KEY_POINT);
        }
        List<String> missed = IntStream.range(0, keyPoints.size())
            .filter(index -> !recalled.contains(index))
            .mapToObj(keyPoints::get)
            .toList();
        recordRepository.save(StudyRecord.recall(source, text, recalled.size() + "/" + keyPoints.size(), now()));
        return new RecallResult(recalled.size(), keyPoints.size(), missed);
    }

    /** LLM을 기다리는 동안 트랜잭션을 열어 두지 않는다. */
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

    @Transactional(readOnly = true)
    public List<RecordView> records(Long ownerAccountId) {
        return recordRepository.findTop50ByOwnerAccountIdOrderByIdDesc(ownerAccountId).stream()
            .map(record -> new RecordView(record.getId(), record.getKind().name(), record.getSourceId(),
                record.getSourceTitle(), record.getPrompt(), record.getUserText(), record.getResult(),
                record.getCreatedAt()))
            .toList();
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
