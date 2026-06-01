package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.application.PostInteractionService;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.support.web.TestExceptionResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostControllerValidationTest {

    @Mock
    private PostCommandService postCommandService;

    @Mock
    private PostQueryService postQueryService;

    @Mock
    private PostInteractionService postInteractionService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;

    private static final String INVALID_POST_REQUEST = """
        {
          "title": "",
          "content": "short"
        }
        """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
            .setControllerAdvice(new TestExceptionResponseHandler())
            .build();
    }

    @Test
    @DisplayName("게시글 생성 요청은 PostRequest 검증을 적용한다")
    void createPost_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_POST_REQUEST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.validation.title").exists())
            .andExpect(jsonPath("$.validation.content").exists());

        verify(postCommandService, never()).savePost(any(), any(), any());
    }

    @Test
    @DisplayName("게시글 수정 요청도 PostRequest 검증을 적용한다")
    void updatePost_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(put("/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_POST_REQUEST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.validation.title").exists())
            .andExpect(jsonPath("$.validation.content").exists());

        verify(postCommandService, never()).updatePost(any(), any(), any());
    }
}
