package dev.iamrat.support.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 전역 advice가 MVC 디스패치 중에 나는 405·415까지 잡으므로, 실제 디스패치로 응답 형태를 확인한다. */
@Tag("webmvc")
class ExceptionResponseHandlerMvcTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
        .setControllerAdvice(new ExceptionResponseHandler())
        .build();

    @Test
    @DisplayName("지원하지 않는 method는 500이 아니라 Allow 헤더가 붙은 405 공통 오류로 응답한다")
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(put("/probe"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", containsString("GET")))
            .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 500이 아니라 415 공통 오류로 응답한다")
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/probe").contentType(MediaType.TEXT_PLAIN).content("hello"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe")
        String read() {
            return "ok";
        }

        @PostMapping("/probe")
        String write(@RequestBody Map<String, String> body) {
            return "ok";
        }
    }
}
