package dev.iamrat.integration;

import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.dto.PostSummaryResponse;
import dev.iamrat.post.repository.PostRepository;
import dev.iamrat.post.service.ViewCountService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private ViewCountService viewCountService;

    @Test
    @WithMockMember
    @DisplayName("게시글 목록을 페이지네이션으로 조회한다")
    void readBoardTest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostDetailResponse> result = postService.getPosts(pageable, "1");

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @WithMockMember
    @DisplayName("게시글 생성 후 조회하면 동일한 데이터를 반환한다")
    @Transactional
    void createAndReadPost() {
        // given
        PostSummaryResponse saved = postService.savePost(
                "테스트 제목", "테스트 내용입니다. 10자 이상.", "testuser", "테스터", List.of());

        // when
        PostDetailResponse detail = postService.getPost(saved.id(), "testuser");

        // then
        assertThat(detail.id()).isEqualTo(saved.id());
        assertThat(detail.title()).isEqualTo("테스트 제목");
        assertThat(detail.content()).isEqualTo("테스트 내용입니다. 10자 이상.");
        assertThat(detail.userId()).isEqualTo("testuser");
    }

    @Test
    @WithMockMember
    @DisplayName("게시글 조회 시 조회수가 증가한다")
    @Transactional
    void viewCountIncrement() {
        // given
        PostSummaryResponse saved = postService.savePost(
                "조회수 테스트", "조회수 증가 테스트 내용입니다.", "testuser", "테스터", List.of());

        // when
        viewCountService.incrementIfNew(saved.id(), "visitor1");
        viewCountService.incrementIfNew(saved.id(), "visitor2");

        // then
        long views = viewCountService.getViewCount(saved.id());
        assertThat(views).isEqualTo(2L);
    }
}
