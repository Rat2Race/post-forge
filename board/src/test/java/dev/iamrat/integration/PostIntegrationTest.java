package dev.iamrat.integration;

import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.repository.PostRepository;
import dev.iamrat.integration.security.WithMockMember;
import dev.iamrat.post.service.PostService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StopWatch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public class PostIntegrationTest {
    
    @Autowired
    MockMvc mockMvc;
 
    @Autowired
    PostRepository postRepository;
    
    @Autowired
    private PostService postService;
    
    @Test
    @WithMockMember
    @DisplayName("게시글을 조회")
    void readBoardTest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        StopWatch sw = new StopWatch("readBoardTest");
        
        sw.start("게시글 조회");
        postService.getPosts(pageable, "1");
        sw.stop();
        
        System.out.println(sw.prettyPrint());
    }
    
    @Test
    @DisplayName("N+1 문제")
    @Transactional
    void checkNPlusOne() {

    }
}
