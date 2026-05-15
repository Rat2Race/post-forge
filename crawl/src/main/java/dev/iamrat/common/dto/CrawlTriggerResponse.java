package dev.iamrat.common.dto;

public record CrawlTriggerResponse(String message) implements CrawlResponse {

    public static CrawlTriggerResponse of(String message) {
        return new CrawlTriggerResponse(message);
    }
}
