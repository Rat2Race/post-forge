package dev.iamrat.study.presentation;

import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.study.application.StudyAiService;
import dev.iamrat.study.application.StudyPracticeService;
import dev.iamrat.study.application.StudyStatsService;
import dev.iamrat.study.application.StudyPracticeService.Today;
import dev.iamrat.study.application.StudyPracticeService.RecallResult;
import dev.iamrat.study.application.StudyPracticeService.RecordView;
import dev.iamrat.study.application.StudyPracticeService.ReviewResult;
import dev.iamrat.study.application.StudySourceService;
import dev.iamrat.study.application.StudySourceService.SourceDetail;
import dev.iamrat.study.application.StudySourceService.SourceSummary;
import dev.iamrat.study.domain.ReviewGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudySourceService sourceService;
    private final StudyPracticeService practiceService;
    private final StudyAiService aiService;
    private final StudyStatsService statsService;

    public record SourceRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        String title,
        @NotBlank(message = "자료 내용은 필수입니다")
        @Size(max = 20000, message = "자료는 20000자 이하여야 합니다")
        String content
    ) {
    }

    public record QuestionRequest(
        @NotBlank(message = "문제는 필수입니다")
        @Size(max = 500, message = "문제는 500자 이하여야 합니다")
        String question,
        @NotBlank(message = "근거는 필수입니다")
        @Size(max = 1000, message = "근거는 1000자 이하여야 합니다")
        String evidence
    ) {
    }

    public record ReviewRequest(
        @Size(max = 10000, message = "답은 10000자 이하여야 합니다")
        String answer,
        @NotNull(message = "자가 평가는 필수입니다")
        ReviewGrade grade
    ) {
    }

    public record RecallRequest(
        @NotBlank(message = "기억나는 내용을 적어 주세요")
        @Size(max = 10000, message = "10000자 이하여야 합니다")
        String text,
        List<Integer> recalledIndexes
    ) {
    }

    public record SuggestRequest(
        @NotBlank(message = "기억나는 내용을 적어 주세요")
        @Size(max = 10000, message = "10000자 이하여야 합니다")
        String text
    ) {
    }

    public record SuggestResponse(List<Integer> mentionedIndexes) {
    }

    public record TeachRequest(
        @NotBlank(message = "설명을 적어 주세요")
        @Size(max = 10000, message = "설명은 10000자 이하여야 합니다")
        String explanation
    ) {
    }

    public record IdResponse(Long id) {
    }

    public record TeachResponse(List<String> questions) {
    }

    @PostMapping("/sources")
    public ResponseEntity<IdResponse> createSource(
        @RequestBody @Valid SourceRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        Long id = sourceService.create(user.getAccountId(), request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    @GetMapping("/sources")
    public List<SourceSummary> getSources(@AuthenticationPrincipal UserPrincipal user) {
        return sourceService.list(user.getAccountId());
    }

    @GetMapping("/sources/{sourceId:\\d+}")
    public SourceDetail getSource(@PathVariable Long sourceId, @AuthenticationPrincipal UserPrincipal user) {
        return sourceService.get(user.getAccountId(), sourceId);
    }

    @PostMapping("/sources/{sourceId:\\d+}/questions")
    public ResponseEntity<IdResponse> addQuestion(
        @PathVariable Long sourceId,
        @RequestBody @Valid QuestionRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        Long id = sourceService.addQuestion(user.getAccountId(), sourceId, request.question(), request.evidence());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    @PostMapping("/sources/{sourceId:\\d+}/recalls")
    public RecallResult recall(
        @PathVariable Long sourceId,
        @RequestBody @Valid RecallRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        List<Integer> recalled = request.recalledIndexes() == null ? List.of() : request.recalledIndexes();
        return practiceService.recall(user.getAccountId(), sourceId, request.text(), recalled);
    }

    @PostMapping("/sources/{sourceId:\\d+}/recalls/suggestions")
    public SuggestResponse suggestRecalled(
        @PathVariable Long sourceId,
        @RequestBody @Valid SuggestRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return new SuggestResponse(practiceService.suggestRecalled(user.getAccountId(), sourceId, request.text()));
    }

    @PostMapping("/sources/{sourceId:\\d+}/teachings")
    public TeachResponse teach(
        @PathVariable Long sourceId,
        @RequestBody @Valid TeachRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return new TeachResponse(aiService.teach(user.getAccountId(), sourceId, request.explanation()));
    }

    @GetMapping("/today")
    public Today getToday(@AuthenticationPrincipal UserPrincipal user) {
        return practiceService.today(user.getAccountId());
    }

    @PostMapping("/questions/{questionId:\\d+}/reviews")
    public ReviewResult review(
        @PathVariable Long questionId,
        @RequestBody @Valid ReviewRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return practiceService.review(user.getAccountId(), questionId, request.answer(), request.grade());
    }

    @PostMapping("/questions/{questionId:\\d+}/follow-ups")
    public ResponseEntity<StudyAiService.FollowUp> followUp(
        @PathVariable Long questionId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiService.followUp(user.getAccountId(), questionId));
    }

    @GetMapping("/stats")
    public StudyStatsService.StudyStats getStats(@AuthenticationPrincipal UserPrincipal user) {
        return statsService.of(user.getAccountId());
    }

    @GetMapping("/records")
    public List<RecordView> getRecords(@AuthenticationPrincipal UserPrincipal user) {
        return practiceService.records(user.getAccountId());
    }
}
