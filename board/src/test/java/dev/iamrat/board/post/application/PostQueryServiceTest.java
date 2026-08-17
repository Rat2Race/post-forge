package dev.iamrat.board.post.application;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.presentation.PostDetailResponse;
import dev.iamrat.board.purchase.application.PurchaseVoteQueryService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostStore postStore;

    @Mock
    private PostReader postReader;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private PurchaseVoteQueryService purchaseVoteQueryService;

    @Mock
    private PostReferenceLinkStore postReferenceLinkStore;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("익명 사용자가 게시글을 읽을 때 조회수 증가는 건너뛴다")
    void readPost_whenAnonymous_skipsViewIncrement() {
        Long postId = 1L;
        Post post = Post.builder()
            .id(postId)
            .title("title")
            .content("content")
            .summary("summary")
            .tags(List.of("tag"))
            .accountId(1L)
            .nickname("writer")
            .build();

        given(postReader.getById(postId)).willReturn(post);
        given(viewCountService.getViewCount(postId)).willReturn(3L);
        given(postLikeService.getLikeInfo(postId, null)).willReturn(new LikeResult(false, 1L));
        given(commentQueryService.getCommentCount(postId)).willReturn(2);
        given(purchaseVoteQueryService.getSummary(post, null)).willReturn(PurchaseVoteSummary.empty(postId, false));
        given(postReferenceLinkStore.findByPostId(postId)).willReturn(List.of());

        PostDetailResponse response = postQueryService.readPost(postId, null);

        assertThat(response.views()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
        assertThat(response.purchaseVote().eligible()).isFalse();
        verify(viewCountService, never()).incrementIfNew(postId, null);
    }

    @Test
    @DisplayName("게시글 상세 조회는 구매 투표 요약을 포함한다")
    void readPost_includesPurchaseVoteSummary() {
        Long postId = 2L;
        Post post = Post.builder()
            .id(postId)
            .title("launch")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
        given(postReader.getById(postId)).willReturn(post);
        given(viewCountService.getViewCount(postId)).willReturn(1L);
        given(postLikeService.getLikeInfo(postId, 9L)).willReturn(new LikeResult(false, 0L));
        given(commentQueryService.getCommentCount(postId)).willReturn(0);
        given(purchaseVoteQueryService.getSummary(post, 9L)).willReturn(new PurchaseVoteSummary(
            postId,
            true,
            2L,
            1L,
            0L,
            PurchaseVoteType.BUYABLE
        ));
        given(postReferenceLinkStore.findByPostId(postId)).willReturn(List.of(referenceLink(post)));

        PostDetailResponse response = postQueryService.getPost(postId, 9L);

        assertThat(response.purchaseVote().eligible()).isTrue();
        assertThat(response.purchaseVote().buyableCount()).isEqualTo(2L);
        assertThat(response.purchaseVote().myVote()).isEqualTo(PurchaseVoteType.BUYABLE);
        assertThat(response.references())
            .singleElement()
            .satisfies(reference -> {
                assertThat(reference.provider()).isEqualTo(PostReferenceProvider.NAVER_NEWS);
                assertThat(reference.canonicalUrl()).isEqualTo("https://news.example/article");
                assertThat(reference.originalUrl()).isEqualTo("https://news.example/article?utm=1");
                assertThat(reference.sourceName()).isEqualTo("Example News");
                assertThat(reference.titleSnapshot()).isEqualTo("갤럭시북 출시");
            });
    }

    @Test
    @DisplayName("게시글 목록 조회는 외부 호출 없이 참조 근거를 포함한다")
    void getPosts_includesReferenceEvidenceWithoutExternalCalls() {
        Post post = Post.builder()
            .id(3L)
            .title("launch")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
        org.springframework.data.domain.Page<Post> page =
            new org.springframework.data.domain.PageImpl<>(List.of(post));
        given(postStore.findByFilters(
            null,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged()
        )).willReturn(page);
        given(postLikeService.getLikedPostIds(List.of(3L), null)).willReturn(java.util.Set.of());
        given(viewCountService.getViewCounts(List.of(3L))).willReturn(Map.of(3L, 5L));
        given(postLikeService.getLikeCounts(List.of(3L))).willReturn(Map.of(3L, 0L));
        given(commentQueryService.getCommentCounts(List.of(3L))).willReturn(Map.of(3L, 0));
        given(purchaseVoteQueryService.getSummaries(List.of(post), null)).willReturn(Map.of(
            3L,
            PurchaseVoteSummary.empty(3L, true)
        ));
        given(postReferenceLinkStore.findByPostIds(List.of(3L))).willReturn(List.of(referenceLink(post)));

        PostDetailResponse response = postQueryService.getPosts(
            null,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged(),
            null
        ).getContent().getFirst();

        assertThat(response.references()).hasSize(1);
        assertThat(response.references().getFirst().canonicalUrl()).isEqualTo("https://news.example/article");
        assertThat(response.purchaseVote().eligible()).isTrue();
        assertThat(response.publishOrigin()).isEqualTo(PostPublishOrigin.USER);
    }

    @Test
    @DisplayName("게시글 목록 조회는 조합 가능한 필터를 store에 위임한다")
    void getPosts_delegatesComposableFiltersToStore() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        given(postStore.findByFilters(
            "갤럭시북",
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostPublishOrigin.SYSTEM_BATCH,
            pageable
        )).willReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        postQueryService.getPosts(
            "  갤럭시북  ",
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostPublishOrigin.SYSTEM_BATCH,
            pageable,
            null
        );

        org.mockito.Mockito.verify(postStore).findByFilters(
            "갤럭시북",
            PostCategory.PRODUCT_LAUNCH_NEWS,
            PostPublishOrigin.SYSTEM_BATCH,
            pageable
        );
    }

    private PostReferenceLink referenceLink(Post post) {
        return PostReferenceLink.builder()
            .id(10L)
            .post(post)
            .keyword("갤럭시북")
            .productId(20L)
            .provider(PostReferenceProvider.NAVER_NEWS)
            .canonicalUrl("https://news.example/article")
            .originalUrl("https://news.example/article?utm=1")
            .sourceName("Example News")
            .publishedAt(LocalDateTime.of(2026, 6, 21, 10, 0))
            .titleSnapshot("갤럭시북 출시")
            .publishOrigin(PostPublishOrigin.SYSTEM_BATCH)
            .build();
    }
}
