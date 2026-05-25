package dev.iamrat.collector.source.presentation;

import dev.iamrat.collector.source.application.SourceCollectionResult;
import dev.iamrat.collector.source.application.SourceCollectionService;
import dev.iamrat.collector.source.presentation.dto.SourceCollectionErrorResponse;
import dev.iamrat.collector.source.presentation.dto.SourceCollectionResponse;
import dev.iamrat.collector.source.presentation.dto.SourceCollectionTriggerResponse;
import dev.iamrat.collector.support.error.CollectorErrorCode;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collector")
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class SourceCollectionController {

    private final SourceCollectionService sourceCollectionService;

    @PostMapping("/{source}")
    public ResponseEntity<SourceCollectionResponse> trigger(@PathVariable String source) {
        SourceCollectionResult result = sourceCollectionService.trigger(source);

        if (!result.collected()) {
            CollectorErrorCode errorCode = CollectorErrorCode.UNKNOWN_SOURCE;
            return ResponseEntity
                    .status(errorCode.getHttpStatus())
                    .body(new SourceCollectionErrorResponse(errorCode.getMessage(), result.availableSources().toString()));
        }

        return ResponseEntity.ok(SourceCollectionTriggerResponse.of(source + " 수집 완료"));
    }
}
