package dev.iamrat.core.board.post;

public record DailyDigestSourceItem(String title, String summary) {
    public DailyDigestSourceItem {
        summary = summary == null ? "" : summary;
    }
}
