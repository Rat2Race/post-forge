package dev.iamrat.source.product.application;

import java.util.List;

public record ProductSourceResult(List<ProductSourceItem> items) {

    public ProductSourceResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
