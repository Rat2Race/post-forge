package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import java.text.Normalizer;
import java.util.Locale;
import java.util.stream.Stream;

public final class ProductEmbeddingTextBuilder {

    private ProductEmbeddingTextBuilder() {
    }

    public static String from(ProductUpsertCommand command) {
        return normalize(Stream.of(
                command.name(),
                command.brand(),
                command.maker(),
                command.category1(),
                command.category2(),
                command.category3()
            )
            .filter(ProductEmbeddingTextBuilder::hasText)
            .reduce((left, right) -> left + " " + right)
            .orElse(command.name()));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
