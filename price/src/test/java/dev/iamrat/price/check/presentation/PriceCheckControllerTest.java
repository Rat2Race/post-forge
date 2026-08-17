package dev.iamrat.price.check.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.price.check.application.PriceCheckBasis;
import dev.iamrat.price.check.application.PriceCheckConfidence;
import dev.iamrat.price.check.application.PriceCheckResult;
import dev.iamrat.price.check.application.PriceCheckSampleItem;
import dev.iamrat.price.check.application.PriceCheckService;
import dev.iamrat.price.check.application.PriceJudgement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
class PriceCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PriceCheckService priceCheckService;

    @Test
    @DisplayName("가격 확인 요청은 판단 응답을 반환한다")
    void postPriceChecksReturnsJudgementResponse() throws Exception {
        given(priceCheckService.check(any())).willReturn(new PriceCheckResult(
            PriceJudgement.NORMAL,
            PriceCheckConfidence.LOW,
            122_000L,
            false,
            "배송비 포함 여부를 확인하지 못해 낮은 확신도로 판정했어요.",
            new PriceCheckBasis("NAVER", 1, 119_000L, 113_050L, 124_950L),
            List.of(new PriceCheckSampleItem("무선 이어폰", "예시몰", 119_000L, "https://shop.example/item"))
        ));

        mockMvc.perform(post("/api/price-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PriceCheckRequest(
                    "무선 이어폰",
                    "https://store.example/item",
                    129_000L,
                    3_000L,
                    10_000L,
                    null
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.judgement").value("NORMAL"))
            .andExpect(jsonPath("$.confidence").value("LOW"))
            .andExpect(jsonPath("$.candidateEffectivePrice").value(122_000))
            .andExpect(jsonPath("$.shippingIncludedVerified").value(false))
            .andExpect(jsonPath("$.basis.provider").value("NAVER"))
            .andExpect(jsonPath("$.items[0].price").value(119_000));
    }

    @Test
    @DisplayName("키워드가 비어 있으면 검증 오류를 반환한다")
    void blankKeywordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/price-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PriceCheckRequest(
                    "",
                    null,
                    10_000L,
                    null,
                    null,
                    null
                ))))
            .andExpect(status().isBadRequest());
    }
}
