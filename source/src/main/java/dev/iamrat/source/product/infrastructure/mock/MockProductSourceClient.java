package dev.iamrat.source.product.infrastructure.mock;

import dev.iamrat.source.product.application.ProductSourceClient;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.domain.SourceType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "source.mock-product",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MockProductSourceClient implements ProductSourceClient {

    @Override
    public boolean supports(SourceType source) {
        return source == SourceType.MOCK;
    }

    @Override
    public ProductSourceResult search(ProductSourceQuery query) {
        String keyword = query.keyword();
        ProductSourceItem item = new ProductSourceItem(
            "mock-" + keyword.toLowerCase().replaceAll("[^a-z0-9가-힣]+", "-"),
            keyword + " 샘플 상품",
            "PostForge",
            "PostForge",
            "테스트",
            "샘플",
            "상품",
            9900L,
            "https://example.com/mock-product.png",
            "https://example.com/products/" + keyword,
            "MockMall"
        );
        return new ProductSourceResult(List.of(item));
    }
}
