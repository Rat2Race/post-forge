package dev.iamrat.stock.controller;

import dev.iamrat.stock.dto.StockMasterUpsertRequest;
import dev.iamrat.stock.entity.StockMaster;
import dev.iamrat.stock.service.StockMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

