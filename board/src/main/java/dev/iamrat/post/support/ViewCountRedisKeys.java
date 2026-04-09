package dev.iamrat.post.support;

public final class ViewCountRedisKeys {
    public static final String VIEW_COUNT_PREFIX = "post:views:";
    public static final String VIEW_DIRTY_KEY = "post:views:dirty";
    public static final String VIEW_GUARD_PREFIX = "post:viewed:";

    private ViewCountRedisKeys() {
    }
}
