package dev.iamrat.source.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NaverSearchRestClientsTest {

    private static class TestProperties extends AbstractNaverSearchProperties {
        TestProperties() {
            super("sim");
        }
    }

    @Test
    @DisplayName("네이버 검색 RestClient는 설정된 base URL과 timeout으로 생성된다")
    void createsClientFromProperties() {
        TestProperties properties = new TestProperties();

        RestClient restClient = NaverSearchRestClients.create(properties);

        assertThat(restClient).isNotNull();
    }
}
