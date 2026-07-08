package dev.iamrat.catalog.matching.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record ProductEmbeddingVector(List<Double> values) {

    private static final String VECTOR_MUST_NOT_BE_EMPTY = "상품 임베딩 벡터는 비어 있을 수 없습니다";

    public ProductEmbeddingVector {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(VECTOR_MUST_NOT_BE_EMPTY);
        }
        values = List.copyOf(values);
    }

    public static ProductEmbeddingVector from(float[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(VECTOR_MUST_NOT_BE_EMPTY);
        }
        return new ProductEmbeddingVector(Arrays.stream(toDoubleArray(values)).boxed().toList());
    }

    public String toPgVectorLiteral() {
        return values.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private static double[] toDoubleArray(float[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }
}
