package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.board.post.PostCategory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private PostStore postStore;

    @Mock
    private PostReader postReader;

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
            viewCountService,
            accountProfileReader
        );
    }

    @Test
    @DisplayName("게시글 생성 시 account profile 포트의 닉네임을 저장한다")
    void savePost_usesAccountProfileNickname() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        PostSummaryResponse response = postCommandService.savePost(
            "title",
            "content",
            1L
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getNickname()).isEqualTo("포트닉네임");
        assertThat(response.nickname()).isEqualTo("포트닉네임");
    }

    @Test
    @DisplayName("게시글 생성 시 요청의 요약, 태그, 카테고리를 저장한다")
    void savePost_savesSummaryTagsAndCategory() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        PostSummaryResponse response = postCommandService.savePost(
            "title",
            "content",
            "summary",
            List.of("tag1", "tag2"),
            PostCategory.GENERAL,
            1L
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getSummary()).isEqualTo("summary");
        assertThat(postCaptor.getValue().getTags()).containsExactly("tag1", "tag2");
        assertThat(postCaptor.getValue().getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(response.summary()).isEqualTo("summary");
        assertThat(response.tags()).containsExactly("tag1", "tag2");
        assertThat(response.category()).isEqualTo(PostCategory.GENERAL);
    }

    @Test
    @DisplayName("게시글 수정 시 제목과 내용을 갱신한다")
    void updatePost_updatesTitleAndContent() {
        Post post = Post.general(
            "old title",
            "old content",
            1L,
            "포트닉네임"
        );
        given(postReader.getById(10L)).willReturn(post);

        PostSummaryResponse response = postCommandService.updatePost(
            10L,
            "new title",
            "new content"
        );

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThat(response.title()).isEqualTo("new title");
    }

    @Test
    @DisplayName("게시글 수정 시 요약, 태그, 카테고리를 함께 갱신한다")
    void updatePost_updatesSummaryTagsAndCategory() {
        Post post = Post.create(
            "old title",
            "old content",
            "old summary",
            List.of("old"),
            PostCategory.GENERAL,
            1L,
            "포트닉네임"
        );
        given(postReader.getById(10L)).willReturn(post);

        PostSummaryResponse response = postCommandService.updatePost(
            10L,
            "new title",
            "new content",
            "new summary",
            List.of("new", "tag"),
            PostCategory.GENERAL
        );

        assertThat(post.getSummary()).isEqualTo("new summary");
        assertThat(post.getTags()).containsExactly("new", "tag");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(response.summary()).isEqualTo("new summary");
        assertThat(response.tags()).containsExactly("new", "tag");
        assertThat(response.category()).isEqualTo(PostCategory.GENERAL);
    }

    @Test
    @DisplayName("게시글 삭제 시 조회수와 저장소 데이터를 정리한다")
    void deletePost_deletesPostAndViewCount() {
        Post post = Post.builder()
            .title("delete title")
            .content("delete content")
            .accountId(1L)
            .nickname("포트닉네임")
            .build();
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.deletePost(10L);

        verify(viewCountService).deleteViewCount(10L);
        verify(postStore).delete(post);
    }
}
