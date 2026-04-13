package dev.iamrat.candidate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "candidate_selections",
    uniqueConstraints = @UniqueConstraint(name = "uk_candidate_run_date_ticker", columnNames = {"runDate", "ticker"})
)
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate runDate;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(length = 100)
    private String stockName;

    @Column(length = 1000)
    private String reason;

    @Column(precision = 8, scale = 4)
    private BigDecimal priceChange;

    @Column(precision = 8, scale = 2)
    private BigDecimal volumeRatio;

    private Long tradingValue;

    private Long marketCap;

    private boolean largeCap;

    private int disclosureCount = 0;

    private int newsCount = 0;

    private int themeKeywordHits = 0;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

