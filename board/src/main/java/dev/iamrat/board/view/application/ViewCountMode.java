package dev.iamrat.board.view.application;

public enum ViewCountMode {
    REDIS("redis_enabled"),
    SQL("sql_only");

    private final String cacheStateLabel;

    ViewCountMode(String cacheStateLabel) {
        this.cacheStateLabel = cacheStateLabel;
    }

    public String cacheStateLabel() {
        return cacheStateLabel;
    }
}
