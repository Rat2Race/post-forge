package dev.iamrat.core.global.dto;

public record UrlResponse(String url) {

    public static UrlResponse of(String url) {
        return new UrlResponse(url);
    }
}
