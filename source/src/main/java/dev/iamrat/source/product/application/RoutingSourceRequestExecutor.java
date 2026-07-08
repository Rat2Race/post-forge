package dev.iamrat.source.product.application;

import dev.iamrat.source.product.domain.SourceType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RoutingSourceRequestExecutor implements SourceRequestExecutor {

    private final Map<SourceType, ProductSourceClient> clients;

    public RoutingSourceRequestExecutor(List<ProductSourceClient> clients) {
        this.clients = new EnumMap<>(SourceType.class);
        for (ProductSourceClient client : clients) {
            for (SourceType source : SourceType.values()) {
                if (client.supports(source)) {
                    this.clients.put(source, client);
                }
            }
        }
    }

    @Override
    public ProductSourceResult search(ProductSourceQuery query) {
        ProductSourceClient client = clients.get(query.source());
        if (client == null) {
            throw new IllegalArgumentException(unsupportedProductSource(query.source()));
        }
        return client.search(query);
    }

    private static String unsupportedProductSource(SourceType source) {
        return "지원하지 않는 상품 소스입니다: " + source;
    }
}
