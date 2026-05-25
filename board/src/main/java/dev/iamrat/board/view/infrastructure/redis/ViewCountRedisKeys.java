package dev.iamrat.board.view.infrastructure.redis;

public final class ViewCountRedisKeys {
    public static final String VIEW_COUNT_PREFIX = "post:views:";
    public static final String VIEW_DIRTY_KEY = "post:views:dirty";
    public static final String VIEW_GUARD_PREFIX = "post:viewed:";

    private ViewCountRedisKeys() {
    }

    public static String viewCountKey(Long postId) {
        return VIEW_COUNT_PREFIX + postId;
    }

    public static String viewGuardKey(Long postId, Long accountId) {
        return VIEW_GUARD_PREFIX + postId + ":" + accountId;
    }

    public static String processingDirtyKey() {
        return VIEW_DIRTY_KEY + ":processing";
    }
}
