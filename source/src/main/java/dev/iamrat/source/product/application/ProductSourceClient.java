package dev.iamrat.source.product.application;

import dev.iamrat.source.product.domain.SourceType;

public interface ProductSourceClient {

    boolean supports(SourceType source);

    ProductSourceResult search(ProductSourceQuery query);
}
