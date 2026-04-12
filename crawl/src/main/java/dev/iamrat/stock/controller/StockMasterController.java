package dev.iamrat.crawl.stock.controller;

import dev.iamrat.crawl.stock.dto.StockMasterUpsertRequest;
import dev.iamrat.crawl.stock.entity.StockMaster;
import dev.iamrat.crawl.stock.service.StockMasterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockMasterController {

    private final StockMasterService stockMasterService;

    @GetMapping
    public ResponseEntity<List<StockMaster>> getStocks() {
        return ResponseEntity.ok(stockMasterService.getAll());
    }

    @GetMapping("/eligible")
    public ResponseEntity<List<StockMaster>> getEligibleStocks() {
        return ResponseEntity.ok(stockMasterService.getEligibleStocks());
    }

    @PostMapping
    public ResponseEntity<StockMaster> upsert(@RequestBody @Valid StockMasterUpsertRequest request) {
        return ResponseEntity.ok(stockMasterService.upsert(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<StockMaster>> bulkUpsert(@RequestBody @Valid List<@Valid StockMasterUpsertRequest> requests) {
        return ResponseEntity.ok(stockMasterService.bulkUpsert(requests));
    }
}
