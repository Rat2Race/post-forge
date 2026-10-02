package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.security.infrastructure.principal.AccountAuthorityMapper;
import dev.iamrat.auth.token.application.TokenIssuer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * OSIV(요청이 끝날 때까지 영속성 컨텍스트를 여는 것)를 끈 채 읽기 API가 모두 200인지 본다.
 * 지연 로딩을 서비스 트랜잭션 밖(컨트롤러·JSON 직렬화)에서 하면 LazyInitializationException으로 500이 된다.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(properties = {
    "spring.config.import=optional:classpath:application-monitoring.yml",
    // 자료 등록 뒤 비동기 문제 생성이 로컬 LLM 게이트웨이에 닿지 않게 닫힌 포트를 준다.
    "LLM_CHAT_BASE_URL=http://127.0.0.1:9"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadEndpointSmokeTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AccountCommandService accountCommandService;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OSIV를 꺼도 게시글·댓글·계정·학습 읽기 API가 모두 200이다")
    void readEndpointsWorkWithoutOpenInView() throws Exception {
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
        Account account = accountCommandService.createGeneralAccount("smokeuser", "Smoke1234!", "smoke@test.local", "스모크");
        String bearer = "Bearer " + tokenIssuer.generateAccessToken(account.getId(), AccountAuthorityMapper.toAuthorityNames(account));

        long postId = createdId(post("/api/posts"), bearer, """
            {"title":"OSIV 확인","content":"영속성 컨텍스트 없이 읽기 확인","tags":["jpa","osiv"]}""");
        long commentId = createdId(post("/api/posts/{postId}/comments", postId), bearer, """
            {"content":"첫 댓글입니다"}""");
        createdId(post("/api/posts/{postId}/comments", postId), bearer, """
            {"parentId":%d,"content":"답글입니다"}""".formatted(commentId));
        mockMvc.perform(post("/api/posts/{postId}/like", postId).header(AUTHORIZATION, bearer));
        mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", postId, commentId).header(AUTHORIZATION, bearer));
        long sourceId = createdId(post("/api/study/sources"), bearer, """
            {"title":"격리 수준","content":"READ COMMITTED는 문장마다 새 스냅샷을 쓰고, REPEATABLE READ는 트랜잭션을 시작할 때 만든 스냅샷을 끝까지 쓴다."}""");

        Map<String, Integer> failures = new LinkedHashMap<>();
        for (String path : List.of(
            "/api/posts", "/api/posts/" + postId, "/api/posts/" + postId + "/comments",
            "/api/user/account", "/api/user/profile",
            "/api/study/sources", "/api/study/sources/" + sourceId, "/api/study/today", "/api/study/stats", "/api/study/records")) {
            record(failures, path + " (로그인)", mockMvc.perform(get(path).header(AUTHORIZATION, bearer)).andReturn().getResponse().getStatus());
        }
        for (String path : List.of("/api/posts", "/api/posts/" + postId, "/api/posts/" + postId + "/comments")) {
            record(failures, path + " (비로그인)", mockMvc.perform(get(path)).andReturn().getResponse().getStatus());
        }

        assertThat(failures).isEmpty();
    }

    private long createdId(MockHttpServletRequestBuilder request, String bearer, String json) throws Exception {
        String body = mockMvc.perform(request.header(AUTHORIZATION, bearer).contentType(APPLICATION_JSON).content(json))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private static void record(Map<String, Integer> failures, String name, int status) {
        if (status != 200) {
            failures.put(name, status);
        }
    }
}
