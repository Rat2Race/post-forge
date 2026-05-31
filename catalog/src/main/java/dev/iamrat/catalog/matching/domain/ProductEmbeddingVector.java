package dev.iamrat.catalog.matching.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record ProductEmbeddingVector(List<Double> values) {

    public ProductEmbeddingVector {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Product embedding vector must not be empty");
        }
        values = List.copyOf(values);
    }

    public static ProductEmbeddingVector from(float[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Product embedding vector must not be empty");
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
