package dev.iamrat.crawl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DartFinancialItem(
        @JsonProperty("rcept_no") String rceptNo,
        @JsonProperty("bsns_year") String bsnsYear,
        @JsonProperty("stock_code") String stockCode,
        @JsonProperty("reprt_code") String reprtCode,
        @JsonProperty("account_nm") String accountNm,
        @JsonProperty("fs_div") String fsDiv,
        @JsonProperty("fs_nm") String fsNm,
        @JsonProperty("sj_div") String sjDiv,
        @JsonProperty("sj_nm") String sjNm,
        @JsonProperty("thstrm_nm") String thstrmNm,
        @JsonProperty("thstrm_amount") String thstrmAmount,
        @JsonProperty("frmtrm_nm") String frmtrmNm,
        @JsonProperty("frmtrm_amount") String frmtrmAmount,
        @JsonProperty("ord") String ord
) {
}
