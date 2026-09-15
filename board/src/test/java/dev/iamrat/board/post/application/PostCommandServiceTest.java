package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.presentation.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private PostStore postStore;

    @Mock
    private PostReader postReader;

    @Mock
    private PostFileAppender postFileAppender;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private AccountProfileReader accountProfileReader;

    private PostCommandService postCommandService;

    @BeforeEach
    void setUp() {
        postCommandService = new PostCommandService(
            postStore,
            postReader,
            postFileAppender,
            viewCountService,
            accountProfileReader
        );
    }

    @Test
    @DisplayName("게시글 생성 시 principal 값이 아니라 account profile 포트의 닉네임을 저장한다")
    void savePost_usesAccountProfileNickname() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        PostSummaryResponse response = postCommandService.savePost(
            "title",
            "content",
            null,
            1L,
            List.of()
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        verify(postFileAppender).appendFiles(postCaptor.getValue(), List.of());
        assertThat(postCaptor.getValue().getNickname()).isEqualTo("포트닉네임");
        assertThat(postCaptor.getValue().getSummary()).isNull();
        assertThat(response.nickname()).isEqualTo("포트닉네임");
    }

    @Test
    @DisplayName("게시글 생성 시 사용자 요약은 저장하지 않는다")
    void savePost_ignoresUserSummary() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        postCommandService.savePost(
            "title",
            "content",
            List.of("tag"),
            1L,
            List.of()
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getSummary()).isNull();
        assertThat(postCaptor.getValue().getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(postCaptor.getValue().getBoardCategory()).isEqualTo(BoardCategory.GENERAL);
    }

    @Test
    @DisplayName("게시글 수정 시 내부 요약을 보존한다")
    void updatePost_preservesExistingSummary() {
        Post post = Post.create(
            "old title",
            "old content",
            "internal summary",
            List.of("old"),
            PostCategory.GENERAL,
            1L,
            "writer"
        );
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.updatePost(
            10L,
            "new title",
            "new content",
            List.of("new"),
            List.of()
        );

        assertThat(post.getSummary()).isEqualTo("internal summary");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        verify(postFileAppender).replaceFiles(post, List.of());
    }

    @Test
    @DisplayName("게시글 수정 시 기존 카테고리를 건드리지 않는다")
    void updatePost_preservesExistingCategory() {
        Post post = Post.create(
            "launch title",
            "launch content",
            "launch summary",
            List.of("launch"),
            PostCategory.PRODUCT_LAUNCH_NEWS,
            1L,
            "writer"
        );
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.updatePost(
            10L,
            "edited title",
            "edited content",
            List.of("edited"),
            List.of()
        );

        assertThat(post.getCategory()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(post.getSummary()).isEqualTo("launch summary");
    }

    @Test
    @DisplayName("게시글 삭제 시 파일 연결 해제와 조회수 캐시 삭제 후 게시글을 삭제한다")
    void deletePost_detachesFilesAndDeletesViewCountAndPost() {
        Post post = Post.builder()
            .title("delete title")
            .content("delete content")
            .accountId(1L)
            .nickname("포트닉네임")
            .build();
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.deletePost(10L);

        verify(postFileAppender).detachFiles(post);
        verify(viewCountService).deleteViewCount(10L);
        verify(postStore).delete(post);
    }
}
