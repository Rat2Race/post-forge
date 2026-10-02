package dev.iamrat.study.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.study.QuestionDraft;
import dev.iamrat.study.application.StudyPracticeService.TodayItem;
import dev.iamrat.study.application.StudyPracticeService.RecallResult;
import dev.iamrat.study.application.StudyPracticeService.RecordView;
import dev.iamrat.study.application.StudySourceService.SourceDetail;
import dev.iamrat.study.domain.ReviewGrade;
import dev.iamrat.study.support.error.StudyErrorCode;
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
        MutableClock clock() {
            return new MutableClock(NOW.atZone(SEOUL).toInstant(), SEOUL);
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
    @Autowired private StudyAiService ai;
    @Autowired private StudyStatsService stats;
    @Autowired private FakeStudyAssistant assistant;
    @Autowired private MutableClock clock;

    private long me;

    @BeforeEach
    void setUp() {
        me = ACCOUNTS.incrementAndGet();
        clock.set(NOW.atZone(SEOUL).toInstant());
        assistant.drafts = List.of();
        assistant.studentQuestions = List.of();
        assistant.followUps = List.of();
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
        assertThat(practice.today(me).items())
            .extracting(TodayItem::question, TodayItem::sourceTitle)
            .containsExactly(tuple("READ COMMITTED는 어떤 데이터를 읽나요?", "격리 수준"));
    }

    @Test
    @DisplayName("LLM이 쓸 만한 질문을 하나도 못 내면 핵심 항목으로 질문을 만든다")
    void fallsBackToRuleQuestionsWhenLlmGivesNothing() {
        sources.create(me, "격리 수준", CONTENT);

        assertThat(practice.today(me).items())
            .hasSize(4)
            .first()
            .extracting(TodayItem::question)
            .isEqualTo("'트랜잭션 격리 수준'에 대해 설명해 보세요.");
    }

    @Test
    @DisplayName("근거는 자료에 그대로 있지만 문제가 너무 길어 버린 초안은 근거 실패로 세지 않는다")
    void tooLongQuestionWithVerbatimEvidenceIsNotAnEvidenceFailure() {
        assistant.drafts = List.of(new QuestionDraft("가".repeat(501), "커밋된 데이터만 읽는다"));

        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        SourceDetail detail = sources.get(me, sourceId);
        assertThat(detail.draftedQuestionCount()).isEqualTo(1);
        assertThat(detail.discardedQuestionCount()).isZero();
    }

    @Test
    @DisplayName("근거로 쓸 문장이 없는 짧은 자료는 문제 0개와 함께 이유를 알려 준다")
    void shortSourceExplainsWhyThereAreNoQuestions() {
        Long sourceId = sources.create(me, "메모", "짧은 메모");

        SourceDetail detail = sources.get(me, sourceId);
        assertThat(detail.questions()).isEmpty();
        assertThat(detail.emptyReason()).isNotBlank();
    }

    @Test
    @DisplayName("LLM 문제를 모두 버려서 0개가 되면 이유에 버린 수가 나온다")
    void emptyReasonMentionsDiscardedDrafts() {
        assistant.drafts = List.of(
            new QuestionDraft("지어낸 문제", "자료에 없는 문장입니다"),
            new QuestionDraft("또 지어낸 문제", "이것도 자료에 없는 문장")
        );

        Long sourceId = sources.create(me, "메모", "짧은 메모");

        assertThat(sources.get(me, sourceId).emptyReason()).contains("2");
    }

    @Test
    @DisplayName("문제가 하나라도 있으면 0개 사유는 비어 있다")
    void noEmptyReasonWhenQuestionsExist() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        assertThat(sources.get(me, sourceId).emptyReason()).isNull();
    }

    @Test
    @DisplayName("남의 자료와 문제는 없는 것처럼 다룬다")
    void hidesOtherAccountsData() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
        long other = ACCOUNTS.incrementAndGet();

        assertThat(practice.today(other).items()).isEmpty();
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

        assertThat(practice.today(me).items()).extracting(TodayItem::id).contains(questionId);
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::prompt)
            .containsExactly(tuple("QUESTION", "팬텀 리드는 언제 생기나요?"));
    }

    @Test
    @DisplayName("복습하면 다음 복습 시각이 미뤄져 오늘 목록에서 빠지고 답변이 기록에 남는다")
    void reviewReschedulesAndRecordsAnswer() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();

        practice.review(me, questionId, "커밋된 것만", ReviewGrade.GOOD);

        assertThat(practice.today(me).items()).isEmpty();
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::prompt, RecordView::userText, RecordView::result)
            .containsExactly(tuple("ANSWER", "무엇을 읽나요?", "커밋된 것만", "GOOD"));
    }

    @Test
    @DisplayName("복습 기록에는 복습 직전의 상자가 남는다 — 하루·7일 간격 유지율의 기준")
    void answerRecordKeepsTheBoxBeforeReview() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();

        practice.review(me, questionId, "커밋된 것만", ReviewGrade.GOOD);
        clock.advance(java.time.Duration.ofDays(1));
        practice.review(me, questionId, "커밋된 것만", ReviewGrade.GOOD);

        assertThat(practice.records(me))
            .extracting(RecordView::reviewBox)
            .containsExactly(1, 0);
    }

    @Test
    @DisplayName("자료마다 LLM이 낸 문제 수와 근거 검증에서 버린 수를 남긴다 — 근거 검증 통과율의 기준")
    void sourceKeepsDraftedAndDiscardedCounts() {
        assistant.drafts = List.of(
            new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"),
            new QuestionDraft("스냅샷은요?", "문장마다 새 스냅샷을 쓴다"),
            new QuestionDraft("지어낸 문제", "자료에 없는 문장입니다")
        );

        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        SourceDetail detail = sources.get(me, sourceId);
        assertThat(detail.draftedQuestionCount()).isEqualTo(3);
        assertThat(detail.discardedQuestionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 문제를 동시에 두 번 제출해도 한 번만 반영된다")
    void concurrentDuplicateReviewIsAppliedOnce() throws Exception {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
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
    @DisplayName("새 자료의 빈 페이지 정리는 다음 날 오늘 할 것에 올라오고, 그 자료의 문제보다 먼저 나온다")
    void recallJoinsTodayFromNextDayBeforeItsQuestions() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        assertThat(practice.today(me).items()).extracting(TodayItem::type).containsExactly("QUESTION");

        clock.advance(java.time.Duration.ofDays(1));

        assertThat(practice.today(me).items())
            .extracting(TodayItem::type, TodayItem::sourceId)
            .containsExactly(tuple("RECALL", sourceId), tuple("QUESTION", sourceId));
    }

    @Test
    @DisplayName("예정된 빈 페이지에서 80% 이상 떠올리면 다음 빈 페이지가 3일 뒤로 미뤄진다")
    void wellRecalledPageMovesThreeDaysLater() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        clock.advance(java.time.Duration.ofDays(1));

        RecallResult result = practice.recall(me, sourceId, "전부 기억나요", List.of(0, 1, 2, 3));

        assertThat(result.nextRecallAt()).isEqualTo(NOW.plusDays(1).plusDays(3));
        assertThat(practice.today(me).items()).extracting(TodayItem::type).doesNotContain("RECALL");
    }

    @Test
    @DisplayName("예정된 빈 페이지에서 절반도 못 떠올리면 다음 날 다시 올라온다")
    void poorlyRecalledPageComesBackNextDay() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        clock.advance(java.time.Duration.ofDays(1));

        RecallResult result = practice.recall(me, sourceId, "하나만 기억나요", List.of(0));

        assertThat(result.nextRecallAt()).isEqualTo(NOW.plusDays(2));
    }

    @Test
    @DisplayName("예정 전에 한 빈 페이지 정리는 기록만 남기고 일정은 바꾸지 않는다")
    void earlyRecallKeepsTheSchedule() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        RecallResult result = practice.recall(me, sourceId, "전부 기억나요", List.of(0, 1, 2, 3));

        assertThat(result.nextRecallAt()).isEqualTo(NOW.plusDays(1));
        assertThat(practice.records(me)).extracting(RecordView::kind).containsExactly("RECALL");
    }

    @Test
    @DisplayName("하루 이상 간격 문제는 예정 시각 전이라도 그날이 되면 오늘 할 것에 나오고 채점된다")
    void dayIntervalQuestionIsDueForTheWholeDay() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
        practice.review(me, questionId, "답", ReviewGrade.GOOD);

        clock.set(NOW.plusDays(1).withHour(7).atZone(SEOUL).toInstant());

        assertThat(practice.today(me).items()).extracting(TodayItem::id).contains(questionId);
        practice.review(me, questionId, "답", ReviewGrade.GOOD);
    }

    @Test
    @DisplayName("10분 간격(첫 칸) 문제는 10분이 지나야 다시 나온다")
    void tenMinuteQuestionWaitsForTheExactTime() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
        practice.review(me, questionId, "답", ReviewGrade.AGAIN);

        clock.advance(java.time.Duration.ofMinutes(5));
        assertThat(practice.today(me).items()).isEmpty();

        clock.advance(java.time.Duration.ofMinutes(6));
        assertThat(practice.today(me).items()).extracting(TodayItem::id).containsExactly(questionId);
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

        List<String> questions = ai.teach(me, sourceId, "READ COMMITTED는 커밋된 데이터만 읽어요.");

        assertThat(questions).containsExactly("스냅샷은 언제 찍나요?");
        assertThat(practice.records(me))
            .extracting(RecordView::kind, RecordView::userText, RecordView::result)
            .containsExactly(tuple("TEACH", "READ COMMITTED는 커밋된 데이터만 읽어요.", "스냅샷은 언제 찍나요?"));
    }

    @Test
    @DisplayName("꼬리질문은 LLM을 한 번 부르고, 근거가 자료에 있는 새 문제를 바로 오늘 할 것에 올린다")
    void followUpCallsLlmOnceAndQueuesVerifiedQuestion() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long parentId = practice.today(me).items().get(0).id();
        practice.review(me, parentId, "커밋된 것", ReviewGrade.GOOD);
        assistant.followUps = List.of(new QuestionDraft("왜 문장마다 스냅샷을 새로 쓰나요?", "문장마다 새 스냅샷을 쓴다"));
        int before = assistant.calls;

        StudyAiService.FollowUp followUp = ai.followUp(me, parentId);

        assertThat(assistant.calls - before).isEqualTo(1);
        assertThat(followUp.origin()).isEqualTo("LLM");
        assertThat(practice.today(me).items()).extracting(TodayItem::id).containsExactly(followUp.id());
        assertThat(practice.records(me)).extracting(RecordView::kind).contains("FOLLOW_UP");
    }

    @Test
    @DisplayName("꼬리질문의 근거가 자료에 없으면 버리고, 앞 문제의 근거로 예를 묻는 문제로 대신한다")
    void followUpFallsBackToExampleQuestionOnUnverifiedEvidence() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        sources.create(me, "격리 수준", CONTENT);
        Long parentId = practice.today(me).items().get(0).id();
        assistant.followUps = List.of(new QuestionDraft("지어낸 꼬리질문", "자료에 없는 문장입니다"));

        StudyAiService.FollowUp followUp = ai.followUp(me, parentId);

        assertThat(followUp.origin()).isEqualTo("RULE");
        assertThat(followUp.evidence()).isEqualTo("커밋된 데이터만 읽는다");
    }

    @Test
    @DisplayName("꼬리질문에는 근거 주변 자료를 보내므로 근거가 자료 뒤쪽에 있어도 문맥에 들어간다")
    void followUpSendsTextAroundEvidenceEvenFarIntoTheSource() {
        String longContent = "# 앞부분\n" + "가나다라마바사 ".repeat(800) + "\n- 뒤쪽 핵심 문장은 여기에 있다\n";
        assistant.drafts = List.of(new QuestionDraft("뒤쪽에는 무엇이 있나요?", "뒤쪽 핵심 문장은 여기에 있다"));
        sources.create(me, "긴 자료", longContent);
        Long parentId = practice.today(me).items().get(0).id();

        ai.followUp(me, parentId);

        assertThat(assistant.lastFollowUpContext).contains("뒤쪽 핵심 문장은 여기에 있다").hasSizeLessThanOrEqualTo(3100);
    }

    @Test
    @DisplayName("꼬리질문은 남의 문제에 만들 수 없다")
    void followUpRejectsOthersQuestion() {
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
        long other = ACCOUNTS.incrementAndGet();

        assertThatThrownBy(() -> ai.followUp(other, questionId))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("학습 현황은 잔디·연속 학습일과 게이트 지표(사용일, 7일 유지율, 근거 검증 통과율)를 기록에서 계산한다")
    void statsComputeStreakAndGateMetricsFromRecords() {
        assistant.drafts = List.of(
            new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"),
            new QuestionDraft("지어낸 문제", "자료에 없는 문장입니다"));
        sources.create(me, "격리 수준", CONTENT);
        Long questionId = practice.today(me).items().get(0).id();
        practice.review(me, questionId, "답", ReviewGrade.GOOD);
        clock.advance(java.time.Duration.ofDays(1));
        practice.review(me, questionId, "답", ReviewGrade.GOOD);
        assistant.followUps = List.of(new QuestionDraft("왜 스냅샷을 새로 쓰나요?", "문장마다 새 스냅샷을 쓴다"));
        ai.followUp(me, questionId);

        StudyStatsService.StudyStats result = stats.of(me);

        java.time.LocalDate day0 = NOW.toLocalDate();
        assertThat(result.today()).isEqualTo(day0.plusDays(1));
        assertThat(result.days()).extracting(StudyStatsService.DayCount::date, StudyStatsService.DayCount::count)
            .containsExactly(tuple(day0, 1), tuple(day0.plusDays(1), 1));
        assertThat(result.streak()).isEqualTo(2);
        assertThat(result.todayDone()).isTrue();
        assertThat(result.activeDaysLast11()).isEqualTo(2);
        assertThat(result.boxCounts()).containsExactly(1, 0, 1, 0, 0, 0);
        assertThat(result.unknown()).isEqualTo(new StudyStatsService.Rate(0, 2));
        assertThat(result.retentionOneDay()).isEqualTo(new StudyStatsService.Rate(1, 1));
        assertThat(result.retentionSevenDay()).isEqualTo(new StudyStatsService.Rate(0, 0));
        assertThat(result.evidencePass()).isEqualTo(new StudyStatsService.Rate(1, 2));
    }

    @Test
    @DisplayName("빈 페이지는 응답마다가 아니라 하루에 3개까지다 — 오늘 한 정리를 빼고 남은 만큼만 다시 받고, 다음 날 다시 3개다")
    void recallCapIsPerDayAcrossRefetches() {
        for (int i = 0; i < 4; i++) {
            sources.create(me, "자료 " + i, CONTENT);
        }
        clock.advance(java.time.Duration.ofDays(1));
        List<TodayItem> recalls = practice.today(me).items().stream().filter(item -> item.type().equals("RECALL")).toList();
        assertThat(recalls).hasSize(3);

        recalls.forEach(item -> practice.recall(me, item.sourceId(), "기억나는 것", List.of()));

        assertThat(practice.today(me).items()).extracting(TodayItem::type).doesNotContain("RECALL");
        clock.advance(java.time.Duration.ofDays(1));
        assertThat(practice.today(me).items()).filteredOn(item -> item.type().equals("RECALL")).hasSize(3);
    }

    @Test
    @DisplayName("매일 반복 루프(오늘 할 것·복습·빈 페이지 제안과 기록)는 LLM을 부르지 않는다")
    void dailyLoopNeverCallsLlm() {
        assistant.drafts = List.of(new QuestionDraft("무엇을 읽나요?", "커밋된 데이터만 읽는다"));
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        int before = assistant.calls;

        Long questionId = practice.today(me).items().get(0).id();
        practice.review(me, questionId, "답", ReviewGrade.GOOD);
        practice.suggestRecalled(me, sourceId, "커밋된 데이터만");
        practice.recall(me, sourceId, "커밋된 데이터만", List.of(1));
        practice.records(me);

        assertThat(assistant.calls).isEqualTo(before);
    }

    @Test
    @DisplayName("AI 학생 질문이 길어도 가르치기 기록은 저장된다")
    void teachingRecordFitsColumnEvenWithLongStudentQuestions() {
        assistant.studentQuestions = List.of("가".repeat(900), "나".repeat(900), "다".repeat(900));
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        ai.teach(me, sourceId, "설명");

        assertThat(practice.records(me))
            .singleElement()
            .satisfies(record -> assertThat(record.result()).hasSizeLessThanOrEqualTo(2000));
    }

    @Test
    @DisplayName("빈 페이지 글에서 언급한 것 같은 핵심 항목을 LLM 없이 제안하고, 기록은 남기지 않는다")
    void suggestsMentionedKeyPointsWithoutLlmOrRecord() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);
        int llmCallsBefore = assistant.calls;

        List<Integer> suggested = practice.suggestRecalled(me, sourceId, "커밋된 데이터만 읽고 팬텀 리드가 생길 수 있어요");

        assertThat(suggested).containsExactly(1, 3);
        assertThat(assistant.calls).isEqualTo(llmCallsBefore);
        assertThat(practice.records(me)).isEmpty();
    }

    @Test
    @DisplayName("빈 페이지 체크 번호에 null이 섞이면 잘못된 항목으로 거절한다")
    void recallRejectsNullIndex() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        assertThatThrownBy(() -> practice.recall(me, sourceId, "글", java.util.Arrays.asList(0, null)))
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(StudyErrorCode.INVALID_KEY_POINT);
    }

    @Test
    @DisplayName("AI 학생이 답하지 못하면 설명에 빠진 핵심 항목을 되묻는다")
    void teachingFallsBackToGapQuestions() {
        Long sourceId = sources.create(me, "격리 수준", CONTENT);

        List<String> questions = ai.teach(me, sourceId,
            "트랜잭션 격리 수준 중 READ COMMITTED는 커밋된 데이터만 읽어요. 문장마다 스냅샷을 새로 찍어요.");

        assertThat(questions).containsExactly("'팬텀 리드가 생길 수 있다' 부분은 설명에 안 나왔어요. 어떤 뜻인가요?");
    }
}
