package dev.iamrat.source.product.infrastructure.naver;

import dev.iamrat.source.product.application.ProductSourceClient;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.domain.SourceType;
import dev.iamrat.source.support.error.SourceExceptionMessages;
import org.springframework.stereotype.Component;

@Component
public class NaverShoppingSourceClient implements ProductSourceClient {

    @Override
    public boolean supports(SourceType source) {
        return source == SourceType.NAVER;
    }

    @Override
    public ProductSourceResult search(ProductSourceQuery query) {
        throw new UnsupportedOperationException(SourceExceptionMessages.naverShoppingSourceRetired());
    }
}
