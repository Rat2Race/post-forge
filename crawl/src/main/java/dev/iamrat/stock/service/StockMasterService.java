package dev.iamrat.stock.service;

import dev.iamrat.stock.dto.StockMasterUpsertRequest;
import dev.iamrat.stock.entity.StockMaster;
import dev.iamrat.stock.repository.StockMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMasterService {

    private final StockMasterRepository stockMasterRepository;

    @Transactional(readOnly = true)
    public List<StockMaster> getAll() {
        return stockMasterRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StockMaster> getEligibleStocks() {
        return stockMasterRepository.findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc();
    }

    @Transactional
    public StockMaster upsert(StockMasterUpsertRequest request) {
        StockMaster stockMaster = new StockMaster(
            request.ticker(),
            request.name(),
            request.englishName(),
            request.market(),
            request.aliases(),
            request.marketCap(),
            request.dartCorpCode(),
            request.isEtf(),
            request.isPreferred(),
            request.isSpac(),
            request.isSuspended(),
            null
        );
        return stockMasterRepository.save(stockMaster);
    }

    @Transactional
    public List<StockMaster> bulkUpsert(List<StockMasterUpsertRequest> requests) {
        return requests.stream()
            .map(this::upsert)
            .toList();
    }
}

