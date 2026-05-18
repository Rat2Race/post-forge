package dev.iamrat.collector.collection.controller;

import dev.iamrat.collector.collection.dto.CollectionErrorResponse;
import dev.iamrat.collector.collection.dto.CollectionResponse;
import dev.iamrat.collector.collection.dto.CollectionTriggerResponse;
import dev.iamrat.collector.collection.service.DataSourceCollector;
import dev.iamrat.core.global.security.OpenApiSecurityPolicy;
import java.util.List;
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
public class CollectionController {

    private final List<DataSourceCollector> collectors;

    @PostMapping("/{source}")
    public ResponseEntity<CollectionResponse> trigger(@PathVariable String source) {
        DataSourceCollector target = collectors.stream()
                .filter(c -> c.getSourceName().equals(source))
                .findFirst()
                .orElse(null);

        if (target == null) {
            List<String> available = collectors.stream()
                    .map(DataSourceCollector::getSourceName)
                    .toList();

            return ResponseEntity
                    .badRequest()
                    .body(new CollectionErrorResponse("Unknown source", available.toString()));
        }

        target.collect();
        return ResponseEntity.ok(CollectionTriggerResponse.of(source + " 수집 완료"));
    }
}
