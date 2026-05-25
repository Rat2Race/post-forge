package dev.iamrat.board.like.application;

public interface LikeRequestWindow {

    boolean markCooldownIfAbsent(String targetType, Long entityId, Long accountId, String action);

    Long incrementRateCount(Long accountId);

    void startRateWindow(Long accountId);
}
