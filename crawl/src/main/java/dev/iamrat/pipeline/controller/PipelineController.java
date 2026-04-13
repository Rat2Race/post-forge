package dev.iamrat.pipeline.controller;

import dev.iamrat.candidate.entity.CandidateSelection;
import dev.iamrat.candidate.service.CandidateSelector;
import dev.iamrat.pipeline.service.CandidatePostPublisher;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final CandidateSelector candidateSelector;
    private final CandidatePostPublisher candidatePostPublisher;
    private final Clock clock;

    @PostMapping("/candidates/select")
    public ResponseEntity<List<CandidateSelection>> selectCandidates(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate
    ) {
        LocalDate targetDate = runDate != null ? runDate : LocalDate.now(clock);
        return ResponseEntity.ok(candidateSelector.select(targetDate));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateSelection>> getCandidates(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate
    ) {
        LocalDate targetDate = runDate != null ? runDate : LocalDate.now(clock);
        return ResponseEntity.ok(candidateSelector.getSelections(targetDate));
    }

    @PostMapping("/posts/generate")
    public ResponseEntity<Map<String, Integer>> generatePosts(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
        @RequestParam(defaultValue = "10") @PositiveOrZero int limit
    ) {
        LocalDate targetDate = runDate != null ? runDate : LocalDate.now(clock);
        List<CandidateSelection> selections = candidateSelector.getSelections(targetDate);
        int count = candidatePostPublisher.publish(selections, limit);
        return ResponseEntity.ok(Map.of("requestedPosts", count));
    }
}

