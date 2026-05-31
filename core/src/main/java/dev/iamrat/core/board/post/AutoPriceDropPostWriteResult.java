package dev.iamrat.core.board.post;

public record AutoPriceDropPostWriteResult(
    Long draftId,
    Long postId,
    boolean published,
    String reason
) {
    public static AutoPriceDropPostWriteResult published(Long draftId, Long postId) {
        return new AutoPriceDropPostWriteResult(draftId, postId, true, "published");
    }

    public static AutoPriceDropPostWriteResult drafted(Long draftId) {
        return new AutoPriceDropPostWriteResult(draftId, null, false, "drafted");
    }

    public static AutoPriceDropPostWriteResult skipped(String reason) {
        return new AutoPriceDropPostWriteResult(null, null, false, reason);
    }
}
