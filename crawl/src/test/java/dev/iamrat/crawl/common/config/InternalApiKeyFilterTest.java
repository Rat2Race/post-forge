package dev.iamrat.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    @Test
    @DisplayName("보호 경로는 내부 API 키가 없으면 차단한다")
    void doFilter_protectedPathWithoutHeader_rejectsRequest() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/crawl/naver-news");
        request.setServletPath("/crawl/naver-news");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
    }

    @Test
    @DisplayName("정상 내부 API 키가 있으면 보호 경로를 통과시킨다")
    void doFilter_protectedPathWithHeader_allowsRequest() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter();
        ReflectionTestUtils.setField(filter, "internalApiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/crawl/naver-news");
        request.setServletPath("/crawl/naver-news");
        request.addHeader("X-Internal-Api-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
