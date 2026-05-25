package dev.iamrat.board.integration;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.post.dto.PostDetailResponse;
import dev.iamrat.board.post.dto.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.board.integration.security.WithMockAccount;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.global.exception.CustomException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class PostIntegrationTest {

    @Autowired
    private PostCommandService postCommandService;

    @Autowired
    private PostQueryService postQueryService;

    @MockitoBean
    private ViewCountService viewCountService;

    @MockitoBean
    private AccountProfileReader accountProfileReader;

    @MockitoBean
    private DomainEventRecorder domainEventRecorder;

    @Test
    @WithMockAccount
    @DisplayName("게시글 생성 후 상세와 목록에서 동일한 데이터를 반환한다")
    @Transactional
    void createAndReadPost() {
        // given
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "테스터"));
        PostSummaryResponse saved = postCommandService.savePost(
                "테스트 제목", "테스트 내용입니다. 10자 이상.", 1L, List.of());
        given(viewCountService.getViewCount(saved.id())).willReturn(0L);
        given(viewCountService.getViewCounts(anyList())).willReturn(Map.of(saved.id(), 0L));

        // when
        PostDetailResponse detail = postQueryService.getPost(saved.id(), 1L);

        // then
        assertThat(detail.id()).isEqualTo(saved.id());
        assertThat(detail.title()).isEqualTo("테스트 제목");
        assertThat(detail.content()).isEqualTo("테스트 내용입니다. 10자 이상.");
        assertThat(detail.accountId()).isEqualTo(1L);
        assertThat(detail.nickname()).isEqualTo("테스터");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostDetailResponse> posts = postQueryService.getPosts(pageable, 1L);

        assertThat(posts.getNumber()).isZero();
        assertThat(posts.getSize()).isEqualTo(20);
        assertThat(posts.getContent())
            .extracting(PostDetailResponse::id)
            .contains(saved.id());
    }

    @Test
    @WithMockAccount
    @DisplayName("게시글 수정 후 상세 조회에 변경된 제목과 내용이 반영된다")
    @Transactional
    void updatePost() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "테스터"));
        PostSummaryResponse saved = postCommandService.savePost(
            "수정 전 제목",
            "수정 전 게시글 내용입니다.",
            1L,
            List.of()
        );
        given(viewCountService.getViewCount(saved.id())).willReturn(0L);

        PostSummaryResponse updated = postCommandService.updatePost(
            saved.id(),
            "수정 후 제목",
            "수정 후 게시글 내용입니다.",
            List.of()
        );
        PostDetailResponse detail = postQueryService.getPost(saved.id(), 1L);

        assertThat(updated.id()).isEqualTo(saved.id());
        assertThat(updated.title()).isEqualTo("수정 후 제목");
        assertThat(detail.title()).isEqualTo("수정 후 제목");
        assertThat(detail.content()).isEqualTo("수정 후 게시글 내용입니다.");
    }

    @Test
    @WithMockAccount
    @DisplayName("게시글 삭제 후 상세 조회 시 게시글을 찾을 수 없다")
    @Transactional
    void deletePost() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "테스터"));
        PostSummaryResponse saved = postCommandService.savePost(
            "삭제 대상 제목",
            "삭제 대상 게시글 내용입니다.",
            1L,
            List.of()
        );

        postCommandService.deletePost(saved.id());

        assertThatThrownBy(() -> postQueryService.getPost(saved.id(), 1L))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }
}
