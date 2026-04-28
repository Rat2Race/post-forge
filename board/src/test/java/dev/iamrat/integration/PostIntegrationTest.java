package dev.iamrat.integration;

import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.dto.PostSummaryResponse;
import dev.iamrat.post.service.ViewCountService;
import dev.iamrat.integration.security.WithMockMember;
import dev.iamrat.post.service.PostService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class PostIntegrationTest {

    @Autowired
    private PostService postService;

    @MockitoBean
    private ViewCountService viewCountService;

    @Test
    @WithMockMember
    @DisplayName("게시글 생성 후 상세와 목록에서 동일한 데이터를 반환한다")
    @Transactional
    void createAndReadPost() {
        // given
        PostSummaryResponse saved = postService.savePost(
                "테스트 제목", "테스트 내용입니다. 10자 이상.", "testuser", "테스터", List.of());
        given(viewCountService.getViewCount(saved.id())).willReturn(0L);
        given(viewCountService.getViewCounts(anyList())).willReturn(Map.of(saved.id(), 0L));

        // when
        PostDetailResponse detail = postService.getPost(saved.id(), "testuser");

        // then
        assertThat(detail.id()).isEqualTo(saved.id());
        assertThat(detail.title()).isEqualTo("테스트 제목");
        assertThat(detail.content()).isEqualTo("테스트 내용입니다. 10자 이상.");
        assertThat(detail.userId()).isEqualTo("testuser");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostDetailResponse> posts = postService.getPosts(pageable, "testuser");

        assertThat(posts.getNumber()).isZero();
        assertThat(posts.getSize()).isEqualTo(20);
        assertThat(posts.getContent())
            .extracting(PostDetailResponse::id)
            .contains(saved.id());
    }
}
