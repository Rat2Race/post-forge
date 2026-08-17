package dev.iamrat.ingest.product.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ingest.product.application.CollectProductsUseCase;
import dev.iamrat.ingest.product.application.TrackedKeywordService;
import dev.iamrat.ingest.product.domain.CollectionJob;
import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.source.product.domain.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductCollectionAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductCollectionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackedKeywordService trackedKeywordService;

    @MockitoBean
    private CollectProductsUseCase collectProductsUseCase;

    @Test
    @DisplayName("키워드 등록과 수동 수집은 같은 상품 수집 요청을 사용한다")
    void productCollectionTarget_isSharedByRegistrationAndManualCollection() throws Exception {
        ProductCollectionTargetRequest request = new ProductCollectionTargetRequest(
            SourceType.NAVER,
            "아이폰",
            10
        );
        given(trackedKeywordService.register(any(), any(), any()))
            .willReturn(TrackedKeyword.register(SourceType.NAVER, "아이폰", 10));

        mockMvc.perform(post("/api/admin/tracked-keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.intervalMinutes").doesNotExist());
    }

    @Test
    @DisplayName("수동 상품 수집은 완료 결과만 반환하고 내부 실패 원인을 노출하지 않는다")
    void runCollection_returnsOkWithoutFailureReason() throws Exception {
        ProductCollectionTargetRequest request = new ProductCollectionTargetRequest(
            SourceType.NAVER,
            "아이폰",
            10
        );
        CollectionJob job = CollectionJob.pending(null, SourceType.NAVER, "아이폰");
        job.markRunning();
        job.markFailed("Naver Shopping 검색 API는 종료되었습니다");
        given(collectProductsUseCase.collect(SourceType.NAVER, "아이폰", 10)).willReturn(job);

        mockMvc.perform(post("/api/admin/collection-jobs/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").doesNotExist());
    }
}
