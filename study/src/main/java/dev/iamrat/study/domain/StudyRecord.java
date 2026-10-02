package dev.iamrat.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학습 활동 한 번의 기록. 덧붙이기만 한다.
 * 자료 제목과 질문 문장은 그때 모습 그대로 남기려고 복사해 둔다.
 */
@Entity
@Table(name = "study_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRecord {

    public enum Kind { ANSWER, RECALL, TEACH, QUESTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerAccountId;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 100)
    private String sourceTitle;

    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(length = 500)
    private String prompt;

    @Column(length = 10000)
    private String userText;

    @Column(length = 2000)
    private String result;

    // 복습 직전 상자. 간격별 유지율(ADR-008 게이트)의 기준이다.
    private Integer reviewBox;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static StudyRecord answer(StudySource source, StudyQuestion question, int boxBeforeReview, String answer,
                                     ReviewGrade grade, LocalDateTime now) {
        StudyRecord record = of(Kind.ANSWER, source, question.getId(), question.getQuestion(), answer, grade.name(), now);
        record.reviewBox = boxBeforeReview;
        return record;
    }

    public static StudyRecord questionMade(StudySource source, StudyQuestion question, LocalDateTime now) {
        return of(Kind.QUESTION, source, question.getId(), question.getQuestion(), null, question.getEvidence(), now);
    }

    public static StudyRecord recall(StudySource source, String text, String result, LocalDateTime now) {
        return of(Kind.RECALL, source, null, null, text, result, now);
    }

    public static StudyRecord teaching(StudySource source, String explanation, String studentQuestions,
                                       LocalDateTime now) {
        return of(Kind.TEACH, source, null, null, explanation, studentQuestions, now);
    }

    private static StudyRecord of(Kind kind, StudySource source, Long questionId, String prompt, String userText,
                                  String result, LocalDateTime now) {
        StudyRecord record = new StudyRecord();
        record.ownerAccountId = source.getOwnerAccountId();
        record.sourceId = source.getId();
        record.sourceTitle = source.getTitle();
        record.questionId = questionId;
        record.kind = kind;
        record.prompt = prompt;
        record.userText = userText;
        record.result = result;
        record.createdAt = now;
        return record;
    }
}
