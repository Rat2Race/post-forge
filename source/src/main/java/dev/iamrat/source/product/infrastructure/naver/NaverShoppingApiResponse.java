package dev.iamrat.source.product.infrastructure.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverShoppingApiResponse(List<NaverShoppingApiItem> items) {

    NaverShoppingApiResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverShoppingApiItem(
    String productId,
    String title,
    String link,
    String image,
    String lprice,
    String mallName,
    String maker,
    String brand,
    String category1,
    String category2,
    String category3
) {
}
