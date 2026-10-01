package dev.iamrat.study.domain;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.study.support.error.StudyErrorCode;
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
@Table(name = "study_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyQuestion {

    public enum Origin { LLM, RULE, USER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Long ownerAccountId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, length = 1000)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origin origin;

    @Column(nullable = false)
    private int box;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    // 같은 문제를 동시에 두 번 채점하면 늦게 커밋하는 쪽이 실패한다.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 어디서 온 문제든 근거가 자료에 그대로 있어야 만들어진다. */
    public static StudyQuestion create(StudySource source, String question, String evidence, Origin origin,
                                       LocalDateTime now) {
        if (!EvidenceVerifier.isQuoted(source.getContent(), evidence)) {
            throw new CustomException(StudyErrorCode.EVIDENCE_NOT_IN_SOURCE);
        }
        StudyQuestion created = new StudyQuestion();
        created.sourceId = source.getId();
        created.ownerAccountId = source.getOwnerAccountId();
        created.question = question;
        created.evidence = evidence;
        created.origin = origin;
        created.box = 0;
        created.dueAt = now;
        created.createdAt = now;
        return created;
    }

    public void review(ReviewGrade grade, LocalDateTime now) {
        if (dueAt.isAfter(now)) {
            throw new CustomException(StudyErrorCode.NOT_DUE_YET);
        }
        ReviewScheduler.Next next = ReviewScheduler.next(box, grade, now);
        this.box = next.box();
        this.dueAt = next.dueAt();
    }
}
