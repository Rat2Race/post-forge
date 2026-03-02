package dev.iamrat.file.controller;

import dev.iamrat.file.dto.FileDto;
import dev.iamrat.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @Test
    @DisplayName("가짜 사진 업로드하면 HTTP 통신 내역 출력")
    void testFileUpload() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test".getBytes());

        given(fileService.uploadFile(mockFile)).willReturn(1L);

        mockMvc.perform(multipart("/files/upload").file(mockFile))
                .andExpect(status().isOk())
                .andExpect(content().string("1"))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 다운로드 시 응답 HTTP 헤더 조립 상태 확인")
    void testFileDownload() throws Exception {
        FileDto fakeDto = new FileDto(
                new ByteArrayResource("<<파일 내용물>>".getBytes()),
                "고양이.jpg",
                "image/jpeg"
        );
        given(fileService.getFile(15L)).willReturn(fakeDto);

        mockMvc.perform(get("/files/15").param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"%EA%B3%A0%EC%96%91%EC%9D%B4.jpg\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(print());
    }
}