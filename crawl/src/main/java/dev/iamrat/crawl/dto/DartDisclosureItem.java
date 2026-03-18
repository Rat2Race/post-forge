package dev.iamrat.crawl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DartDisclosureItem(
        @JsonProperty("corp_code") String corpCode,
        @JsonProperty("corp_name") String corpName,
        @JsonProperty("stock_code") String stockCode,
        @JsonProperty("corp_cls") String corpCls,
        @JsonProperty("report_nm") String reportNm,
        @JsonProperty("rcept_no") String rceptNo,
        @JsonProperty("flr_nm") String flrNm,
        @JsonProperty("rcept_dt") String rceptDt,
        String rm
) {
}
