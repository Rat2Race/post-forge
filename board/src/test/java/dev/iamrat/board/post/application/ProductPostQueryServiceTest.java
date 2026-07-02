package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostProductLink;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.infrastructure.persistence.PostProductLinkRepository;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.purchase.application.PurchaseVoteQueryService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductPostQueryServiceTest {

    @Mock
    private PostProductLinkRepository postProductLinkRepository;

    @Mock
    private PurchaseVoteQueryService purchaseVoteQueryService;

    @Mock
    private PostReferenceLinkStore postReferenceLinkStore;

    @InjectMocks
    private ProductPostQueryService productPostQueryService;

    @Test
    @DisplayName("상품 연결 게시글은 상품 링크 저장소만으로 조회한다")
    void getProductPosts_readsProductLinksWithoutAutoWriter() {
        Post post = Post.builder()
            .id(1L)
            .title("상품 후기")
            .content("좋아요")
            .accountId(1L)
            .nickname("writer")
            .build();
        PostProductLink link = PostProductLink.of(post, 10L, LocalDate.of(2026, 6, 16));
        given(postProductLinkRepository.findByProductIdOrderByCreatedAtDesc(10L)).willReturn(List.of(link));
        given(purchaseVoteQueryService.getSummaries(List.of(post), null)).willReturn(Map.of(
            post.getId(),
            PurchaseVoteSummary.empty(post.getId(), false)
        ));
        given(postReferenceLinkStore.findByPostIds(List.of(post.getId()))).willReturn(List.of(referenceLink(post)));

        List<PostDetailResponse> responses = productPostQueryService.getProductPosts(10L);

        assertThat(responses)
            .singleElement()
            .satisfies(response -> {
                assertThat(response.title()).isEqualTo("상품 후기");
                assertThat(response.content()).isEqualTo("좋아요");
                assertThat(response.isLiked()).isFalse();
                assertThat(response.purchaseVote().eligible()).isFalse();
                assertThat(response.references()).hasSize(1);
                assertThat(response.references().getFirst().canonicalUrl()).isEqualTo("https://news.example/article");
            });
        verify(postProductLinkRepository).findByProductIdOrderByCreatedAtDesc(10L);
    }

    private PostReferenceLink referenceLink(Post post) {
        return PostReferenceLink.builder()
            .id(10L)
            .post(post)
            .keyword("갤럭시북")
            .productId(10L)
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
