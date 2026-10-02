package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
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
    @DisplayName("게시글 생성 시 principal 값이 아니라 account profile 포트의 닉네임을 저장한다")
    void savePost_usesAccountProfileNickname() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        PostSummary response = postCommandService.savePost(
            "title",
            "content",
            null,
            1L
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getNickname()).isEqualTo("포트닉네임");
        assertThat(response.nickname()).isEqualTo("포트닉네임");
    }

    @Test
    @DisplayName("게시글 수정 시 제목·내용·태그를 바꾼다")
    void updatePost_changesFields() {
        Post post = Post.create("old title", "old content", List.of("old"), 1L, "writer");
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.updatePost(
            10L,
            "new title",
            "new content",
            List.of("new")
        );

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getTags()).containsExactly("new");
    }

    @Test
    @DisplayName("게시글 삭제 시 조회수 캐시를 지운 뒤 게시글을 삭제한다")
    void deletePost_deletesViewCountAndPost() {
        Post post = Post.builder()
            .title("delete title")
            .content("delete content")
            .accountId(1L)
            .nickname("포트닉네임")
            .build();
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.deletePost(10L);

        verify(viewCountService).deleteViewCount(10L);
        verify(postStore).delete(post);
    }
}
