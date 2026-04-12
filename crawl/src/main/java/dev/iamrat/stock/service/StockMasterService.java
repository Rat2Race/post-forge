package dev.iamrat.crawl.stock.service;

import dev.iamrat.crawl.stock.dto.StockMasterUpsertRequest;
import dev.iamrat.crawl.stock.entity.StockMaster;
import dev.iamrat.crawl.stock.repository.StockMasterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
