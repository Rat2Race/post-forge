package dev.iamrat.crawl.price.repository;

import dev.iamrat.crawl.price.entity.StockPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    Optional<StockPrice> findByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    Optional<StockPrice> findTopByTickerAndTradeDateLessThanEqualOrderByTradeDateDesc(String ticker, LocalDate tradeDate);

    List<StockPrice> findTop20ByTickerAndTradeDateBeforeOrderByTradeDateDesc(String ticker, LocalDate tradeDate);
}
