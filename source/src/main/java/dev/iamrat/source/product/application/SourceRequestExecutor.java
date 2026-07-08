package dev.iamrat.source.product.application;

public interface SourceRequestExecutor {

    ProductSourceResult search(ProductSourceQuery query);
}
