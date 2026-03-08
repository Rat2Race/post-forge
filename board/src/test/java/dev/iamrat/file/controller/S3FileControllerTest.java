package dev.iamrat.file.controller;

import dev.iamrat.file.dto.FileUploadResponse;
import dev.iamrat.file.service.S3FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("s3")
class S3FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3FileService s3FileService;

    @Test
    @DisplayName("Presigned URL 발급 요청 시 FileUploadResponse 반환")
    void testGetPresignedUrl() throws Exception {
        FileUploadResponse response = new FileUploadResponse(1L, "uuid.jpg", "https://s3.presigned-url.example.com");
        given(s3FileService.createPresignedUrl("test.jpg", "image/jpeg")).willReturn(response);

        mockMvc.perform(get("/files/s3/presigned-url")
                        .param("fileName", "test.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.savedName").value("uuid.jpg"))
                .andExpect(jsonPath("$.url").value("https://s3.presigned-url.example.com"))
                .andDo(print());
    }

    @Test
    @DisplayName("다운로드 URL 요청 시 Presigned Download URL 반환")
    void testGetDownloadUrl() throws Exception {
        given(s3FileService.createDownloadUrl(1L)).willReturn("https://s3.download-url.example.com");

        mockMvc.perform(get("/files/s3/1/download-url"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://s3.download-url.example.com"))
                .andDo(print());
    }
}
