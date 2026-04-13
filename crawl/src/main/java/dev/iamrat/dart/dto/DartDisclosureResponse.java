package dev.iamrat.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DartDisclosureResponse(
        String status,
        String message,
        @JsonProperty("page_no") int pageNo,
        @JsonProperty("page_count") int pageCount,
        @JsonProperty("total_count") int totalCount,
        @JsonProperty("total_page") int totalPage,
        List<DartDisclosureItem> list
) {
}

