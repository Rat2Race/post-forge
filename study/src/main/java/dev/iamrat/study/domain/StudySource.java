package dev.iamrat.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    // 자료마다 빈 페이지 정리를 따로 간격 반복한다.
    @Column(nullable = false)
    private int recallBox;

    @Column(nullable = false)
    private LocalDateTime recallDueAt;

    // 문제 생성 완료와 빈 페이지 일정 갱신이 겹쳐도 한쪽 변경이 덮이지 않게 한다.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static StudySource create(Long ownerAccountId, String title, String content, LocalDateTime now) {
        StudySource source = new StudySource();
        source.ownerAccountId = ownerAccountId;
        source.title = title;
        source.content = content;
        source.questionStatus = QuestionStatus.GENERATING;
        source.recallBox = 0;
        source.recallDueAt = ReviewScheduler.firstRecallAt(now);
        source.createdAt = now;
        return source;
    }

    /** 하루 이상 간격이라 예정일이 되면 그날 내내 할 수 있다. */
    public boolean isRecallDue(LocalDateTime now) {
        return !recallDueAt.toLocalDate().isAfter(now.toLocalDate());
    }

    /** 예정된 빈 페이지일 때만 일정을 옮긴다. 예정 전에 한 정리는 기록만 남는다. */
    public void recalled(ReviewGrade grade, LocalDateTime now) {
        if (!isRecallDue(now)) {
            return;
        }
        ReviewScheduler.Next next = ReviewScheduler.nextRecall(recallBox, grade, now);
        this.recallBox = next.box();
        this.recallDueAt = next.dueAt();
    }

    public void questionsReady(int draftedQuestionCount, int discardedQuestionCount) {
        this.questionStatus = QuestionStatus.READY;
        this.draftedQuestionCount = draftedQuestionCount;
        this.discardedQuestionCount = discardedQuestionCount;
    }

    /** 꼬리질문에서 LLM이 낸 초안도 근거 검증 통과율(ADR-008 게이트)에 더한다. */
    public void followUpDrafted(int draftedQuestionCount, int discardedQuestionCount) {
        this.draftedQuestionCount += draftedQuestionCount;
        this.discardedQuestionCount += discardedQuestionCount;
    }
}
