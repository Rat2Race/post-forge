package dev.iamrat.board.like.infrastructure.redis;

final class LikeRequestRedisKeys {

    private LikeRequestRedisKeys() {
    }

    static String cooldownKey(String targetType, Long entityId, Long accountId, String action) {
        return "like:cooldown:" + targetType + ":" + action + ":" + entityId + ":" + accountId;
    }

    static String rateKey(Long accountId) {
        return "like:rate:" + accountId;
    }
}
