package dev.iamrat.core.global.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    @DisplayName("Page를 API 응답 DTO로 변환한다")
    void from_convertsPageToResponse() {
        PageImpl<String> page = new PageImpl<>(
            List.of("first", "second"),
            PageRequest.of(1, 2),
            5
        );

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("first", "second");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.numberOfElements()).isEqualTo(2);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.empty()).isFalse();
    }
}
