package dev.iamrat.price.check.application;

final class PriceCheckExceptionMessages {

    static final String KEYWORD_MUST_NOT_BE_BLANK = "키워드는 비어 있을 수 없습니다";
    static final String BASE_PRICE_REQUIRED_UNLESS_FINAL_PAID_PRICE_PROVIDED =
        "finalPaidPrice가 없으면 basePrice는 필수입니다";
    static final String CANDIDATE_EFFECTIVE_PRICE_MUST_NOT_BE_NEGATIVE =
        "후보 유효 가격은 음수일 수 없습니다";

    private PriceCheckExceptionMessages() {
    }

    static String mustNotBeNegative(String field) {
        return displayName(field) + "은(는) 음수일 수 없습니다";
    }

    private static String displayName(String field) {
        return switch (field) {
            case "basePrice" -> "기준 가격";
            case "shippingFee" -> "배송비";
            case "discountAmount" -> "할인 금액";
            case "finalPaidPrice" -> "최종 결제 가격";
            default -> field;
        };
    }
}
