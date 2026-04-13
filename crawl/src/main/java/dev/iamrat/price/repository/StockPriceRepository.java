package dev.iamrat.price.repository;

import dev.iamrat.price.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    Optional<StockPrice> findByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    Optional<StockPrice> findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc(String ticker, LocalDate tradeDate);

    List<StockPrice> findTop20ByTickerAndTradeDateBeforeOrderByTradeDateDesc(String ticker, LocalDate tradeDate);
}

