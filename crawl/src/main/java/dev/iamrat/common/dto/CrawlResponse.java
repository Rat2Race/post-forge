package dev.iamrat.common.dto;

public sealed interface CrawlResponse permits CrawlErrorResponse, CrawlTriggerResponse {
}
