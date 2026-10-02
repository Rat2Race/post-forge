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

@Entity
@Table(name = "study_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudySource {

    public enum QuestionStatus { GENERATING, READY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerAccountId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 20000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus questionStatus;

    // LLM이 낸 문제 초안 수. 근거 검증 통과율(ADR-008 게이트)의 분모다.
    @Column(nullable = false)
    private int draftedQuestionCount;

    @Column(nullable = false)
    private int discardedQuestionCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static StudySource create(Long ownerAccountId, String title, String content, LocalDateTime now) {
        StudySource source = new StudySource();
        source.ownerAccountId = ownerAccountId;
        source.title = title;
        source.content = content;
        source.questionStatus = QuestionStatus.GENERATING;
        source.createdAt = now;
        return source;
    }

    public void questionsReady(int draftedQuestionCount, int discardedQuestionCount) {
        this.questionStatus = QuestionStatus.READY;
        this.draftedQuestionCount = draftedQuestionCount;
        this.discardedQuestionCount = discardedQuestionCount;
    }
}
