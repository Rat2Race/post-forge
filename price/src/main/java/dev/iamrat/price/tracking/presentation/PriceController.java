package dev.iamrat.price.tracking.presentation;

import dev.iamrat.price.tracking.application.PriceSnapshotService;
import dev.iamrat.price.tracking.domain.PriceDropPolicy;
import dev.iamrat.price.tracking.presentation.dto.PriceDropResponse;
import dev.iamrat.price.tracking.presentation.dto.PriceSnapshotResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PriceController {

    private final PriceSnapshotService priceSnapshotService;

    @GetMapping("/api/products/{productId:\\d+}/prices")
    public ResponseEntity<List<PriceSnapshotResponse>> getPriceHistory(
        @PathVariable Long productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(priceSnapshotService.getHistory(productId, from, to).stream()
            .map(PriceSnapshotResponse::from)
            .toList());
    }

    @GetMapping("/api/products/price-drops")
    public ResponseEntity<List<PriceDropResponse>> getPriceDrops(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "10") BigDecimal minDropRate
    ) {
        return ResponseEntity.ok(priceSnapshotService.getPriceDrops(limit, new PriceDropPolicy(minDropRate)).stream()
            .map(PriceDropResponse::from)
            .toList());
    }
}
