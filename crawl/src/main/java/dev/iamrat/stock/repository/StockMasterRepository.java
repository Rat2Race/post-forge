package dev.iamrat.stock.repository;

import dev.iamrat.stock.entity.StockMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

    List<StockMaster> findByIsEtfFalseAndIsPreferredFalseAndIsSpacFalseAndIsSuspendedFalseOrderByMarketCapDesc();
}

