package dev.iamrat.crawl.dto;

import java.util.List;

public record DartFinancialResponse(
        String status,
        String message,
        List<DartFinancialItem> list
) {
}
