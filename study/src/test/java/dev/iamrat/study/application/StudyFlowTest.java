package dev.iamrat.study.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.study.application.StudyPracticeService.DueQuestion;
import dev.iamrat.study.application.StudyPracticeService.RecallResult;
import dev.iamrat.study.application.StudyPracticeService.RecordView;
import dev.iamrat.study.application.StudySourceService.SourceDetail;
import dev.iamrat.study.domain.ReviewGrade;
import dev.iamrat.study.support.error.StudyErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(StudyFlowTest.Config.class)
class StudyFlowTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final AtomicLong ACCOUNTS = new AtomicLong();
    private static final String CONTENT = """
        # 트랜잭션 격리 수준
        - 커밋된 데이터만 읽는다
        - 문장마다 새 스냅샷을 쓴다
        - 팬텀 리드가 생길 수 있다
        """;

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        }

        @Bean
        FakeStudyAssistant studyAssistant() {
            return new FakeStudyAssistant();
        }

        @Bean
        Executor applicationTaskExecutor() {
            return Runnable::run;
        }
    }

    @Autowired private StudySourceService sources;
    @Autowired private StudyPracticeService practice;
    @Autowired private FakeStudyAssistant assistant;

    private long me;

    @BeforeEach
    void setUp() {
        me = ACCOUNTS.incrementAndGet();
        assistant.drafts = List.of();
        assistant.studentQuestions = List.of();
    }

    @Test
    @DisplayName("LLM 질문 중 근거가 자료에 없는 것은 버리고 남은 질문을 바로 오늘 복습 목록에 올린다")
    void keepsOnlyQuestionsWithVerbatimEvidence() {
        assistant.drafts = List.of(
            new QuestionDraft("READ COMMITTED는 어떤 데이터를 읽나요?", "커밋된 데이터만 읽는다"),
            new QuestionDraft("팬텀 리드는 왜 생기나요?", "범위 잠금이 없어서 팬텀 리드가 생긴다")
        );

        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        SourceDetail detail = sources.get(me, sourceId);
        assertThat(detail.questionStatus()).isEqualTo("READY");
        assertThat(detail.discardedQuestionCount()).isEqualTo(1);
        assertThat(practice.today(me))
            .extracting(DueQuestion::question, DueQuestion::sourceTitle)
            .containsExactly(tuple("READ COMMITTED는 어떤 데이터를 읽나요?", "격리 수준"));
    }

    @Test
    @DisplayName("LLM이 쓸 만한 질문을 하나도 못 내면 핵심 항목으로 질문을 만든다")
    void fallsBackToRuleQuestionsWhenLlmGivesNothing() {
        sources.create(me, "격리 수준", CONTENT);

        assertThat(practice.today(me))
            .hasSize(4)
            .first()
            .extracting(DueQuestion::question)
            .isEqualTo("'트랜잭션 격리 수준'에 대해 설명해 보세요.");
    }

    @Test
    @DisplayName("남의 자료와 문제는 없는 것처럼 다룬다")
    void hidesOtherAccountsData() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).get(0).id();
        long other = ACCOUNTS.incrementAndGet();

        assertThat(practice.today(other)).isEmpty();
        assertThatThrownBy(() -> sources.get(other, sourceId))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.SOURCE_NOT_FOUND);
        assertThatThrownBy(() -> practice.review(other, questionId, "답", ReviewGrade.GOOD))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("내가 만든 문제도 근거가 자료에 그대로 없으면 받지 않고, 있으면 복습 목록에 올린다")
    void userQuestionNeedsVerbatimEvidence() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        assertThatThrownBy(() -> sources.addQuestion(me, sourceId, "팬텀 리드란?", "팬텀 리드는 유령 같은 행이다"))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.EVIDENCE_NOT_IN_SOURCE);

        Long questionId = sources.addQuestion(me, sourceId, "팬텀 리드는 언제 생기나요?", "팬텀 리드가 생길 수 있다");

        assertThat(practice.today(me)).extracting(DueQuestion::id).contains(questionId);
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::prompt)
            .containsExactly(tuple("QUESTION", "팬텀 리드는 언제 생기나요?"));
    }

    @Test
    @DisplayName("복습하면 다음 복습 시각이 미뤄져 오늘 목록에서 빠지고 답변이 기록에 남는다")
    void reviewReschedulesAndRecordsAnswer() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).get(0).id();

        practice.review(me, questionId, "커밋된 것만", ReviewGrade.GOOD);

        assertThat(practice.today(me)).isEmpty();
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::prompt, RecordView::userText, RecordView::result)
            .containsExactly(tuple("ANSWER", "무엇을 읽나요?", "커밋된 것만", "GOOD"));
    }

    @Test
    @DisplayName("같은 문제를 동시에 두 번 제출해도 한 번만 반영된다")
    void concurrentDuplicateReviewIsAppliedOnce() throws Exception {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).get(0).id();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> submit = () -> {
            start.await();
            try {
                practice.review(me, questionId, "답", ReviewGrade.GOOD);
                return true;
            } catch (RuntimeException rejected) {
                return false;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(submit);
            Future<Boolean> second = pool.submit(submit);
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
        assertThat(practice.records(me)).hasSize(1);
        assertThat(sources.get(me, practice.records(me).get(0).sourceId()).questions())
            .singleElement()
            .satisfies(question -> assertThat(question.box()).isEqualTo(1));
    }

    @Test
    @DisplayName("빈 페이지 정리는 체크한 핵심 항목 수를 기록하고 빠뜨린 항목을 돌려준다")
    void recallRecordsCheckedKeyPoints() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        RecallResult result = practice.recall(me, sourceId, "격리 수준은 커밋된 것만 읽는다", List.of(0, 1));

        assertThat(result.recalled()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(4);
        assertThat(result.missed()).containsExactly("문장마다 새 스냅샷을 쓴다", "팬텀 리드가 생길 수 있다");
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::result)
            .containsExactly(tuple("RECALL", "2/4"));
        assertThatThrownBy(() -> practice.recall(me, sourceId, "글", List.of(4)))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.INVALID_KEY_POINT);
    }

    @Test
    @DisplayName("가르치기는 AI 학생의 질문을 돌려주고 설명과 함께 기록한다")
    void teachingReturnsStudentQuestions() {
        assistant.studentQuestions = List.of("스냅샷은 언제 찍나요?");
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        List<String> questions = practice.teach(me, sourceId, "READ COMMITTED는 커밋된 데이터만 읽어요.");

        assertThat(questions).containsExactly("스냅샷은 언제 찍나요?");
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::userText, RecordView::result)
            .containsExactly(tuple("TEACH", "READ COMMITTED는 커밋된 데이터만 읽어요.", "스냅샷은 언제 찍나요?"));
    }

    @Test
    @DisplayName("AI 학생이 답하지 못하면 설명에 빠진 핵심 항목을 되묻는다")
    void teachingFallsBackToGapQuestions() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        List<String> questions = practice.teach(me, sourceId,
            "트랜잭션 격리 수준 중 READ COMMITTED는 커밋된 데이터만 읽어요. 문장마다 스냅샷을 새로 찍어요.");

        assertThat(questions).containsExactly("'팬텀 리드가 생길 수 있다' 부분은 설명에 안 나왔어요. 어떤 뜻인가요?");
    }
}
