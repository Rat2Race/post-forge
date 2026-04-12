package dev.iamrat.crawl.stock.repository;

import dev.iamrat.crawl.stock.entity.StockMaster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

    List<StockMaster> findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc();
}
