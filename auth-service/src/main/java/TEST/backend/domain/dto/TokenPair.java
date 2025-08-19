package TEST.backend.domain.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
