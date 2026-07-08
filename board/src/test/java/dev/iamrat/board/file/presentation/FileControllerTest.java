package dev.iamrat.board.file.presentation;

import dev.iamrat.board.file.application.FileReader;
import dev.iamrat.board.file.application.FileUploadResult;
import dev.iamrat.board.file.application.FileUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private FileReader fileReader;

    @Test
    @DisplayName("스토리지 중립 경로에서 사전 서명 URL 발급 요청 시 FileUploadResult를 반환한다")
    void getPresignedUrl_storageNeutralPath_returnsFileUploadResult() throws Exception {
        FileUploadResult response = new FileUploadResult(1L, "uuid.jpg", "https://s3.presigned-url.example.com");
        given(fileUploadService.createPresignedUrl("test.jpg", "image/jpeg")).willReturn(response);

        mockMvc.perform(get("/api/files/presigned-url")
                        .param("fileName", "test.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.savedName").value("uuid.jpg"))
                .andExpect(jsonPath("$.url").value("https://s3.presigned-url.example.com"));
    }

    @Test
    @DisplayName("S3 경로도 사전 서명 URL 발급을 유지한다")
    void getPresignedUrl_s3Path_returnsFileUploadResult() throws Exception {
        FileUploadResult response = new FileUploadResult(1L, "uuid.jpg", "https://s3.presigned-url.example.com");
        given(fileUploadService.createPresignedUrl("test.jpg", "image/jpeg")).willReturn(response);

        mockMvc.perform(get("/api/files/s3/presigned-url")
                        .param("fileName", "test.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.savedName").value("uuid.jpg"))
                .andExpect(jsonPath("$.url").value("https://s3.presigned-url.example.com"));
    }

    @Test
    @DisplayName("스토리지 중립 경로에서 다운로드 URL 요청 시 사전 서명 다운로드 URL을 반환한다")
    void getDownloadUrl_storageNeutralPath_returnsDownloadUrl() throws Exception {
        given(fileReader.createDownloadUrl(1L)).willReturn("https://s3.download-url.example.com");

        mockMvc.perform(get("/api/files/1/download-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://s3.download-url.example.com"));
    }
}
