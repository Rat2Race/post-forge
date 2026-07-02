package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.event.PostCreatedEvent;
import dev.iamrat.board.post.domain.event.PostDeletedEvent;
import dev.iamrat.board.post.domain.event.PostDomainEvent;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private DomainEventRecorder domainEventRecorder;

    private PostCommandService postCommandService;

    @BeforeEach
    void setUp() {
        postCommandService = new PostCommandService(
            postStore,
            postReader,
            postFileAppender,
            viewCountService,
            accountProfileReader,
            List.of(domainEventRecorder)
        );
    }

    @Test
    @DisplayName("게시글 생성 시 principal 값이 아니라 account profile 포트의 닉네임을 저장한다")
    void savePost_usesAccountProfileNickname() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        PostSummaryResponse response = postCommandService.savePost(
            "title",
            "content",
            1L,
            List.of()
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        verify(postFileAppender).appendFiles(postCaptor.getValue(), List.of());
        assertThat(postCaptor.getValue().getNickname()).isEqualTo("포트닉네임");
        assertThat(postCaptor.getValue().getSummary()).isNull();
        assertThat(postCaptor.getValue().getBoardCategory()).isEqualTo(PostBoardCategory.GENERAL);
        assertThat(response.nickname()).isEqualTo("포트닉네임");
    }

    @Test
    @DisplayName("게시글 생성 시 게시판 카테고리를 저장하고 사용자 요약은 저장하지 않는다")
    void savePost_storesBoardCategoryAndIgnoresUserSummary() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        postCommandService.savePost(
            "title",
            "content",
            List.of("tag"),
            PostBoardCategory.HEALTH,
            1L,
            List.of()
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getSummary()).isNull();
        assertThat(postCaptor.getValue().getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(postCaptor.getValue().getBoardCategory()).isEqualTo(PostBoardCategory.HEALTH);
    }

    @Test
    @DisplayName("게시글 생성 시 boardCategory가 없으면 태그로 과도기 추론한다")
    void savePost_infersBoardCategoryFromTagsWhenMissing() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));

        postCommandService.savePost(
            "title",
            "content",
            List.of("가전"),
            null,
            1L,
            List.of()
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postStore).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(postCaptor.getValue().getBoardCategory()).isEqualTo(PostBoardCategory.APPLIANCE);
    }

    @Test
    @DisplayName("게시글 수정 시 내부 요약을 보존하고 게시판 카테고리를 갱신한다")
    void updatePost_preservesExistingSummaryAndUpdatesBoardCategory() {
        Post post = Post.create(
            "old title",
            "old content",
            "internal summary",
            List.of("old"),
            PostCategory.GENERAL,
            PostBoardCategory.GENERAL,
            1L,
            "writer"
        );
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.updatePost(
            10L,
            "new title",
            "new content",
            List.of("new"),
            PostBoardCategory.BEAUTY,
            List.of()
        );

        assertThat(post.getSummary()).isEqualTo("internal summary");
        assertThat(post.getCategory()).isEqualTo(PostCategory.GENERAL);
        assertThat(post.getBoardCategory()).isEqualTo(PostBoardCategory.BEAUTY);
        verify(postFileAppender).replaceFiles(post, List.of());
    }

    @Test
    @DisplayName("게시글 생성 시 같은 트랜잭션 안에서 PostCreated 이벤트 기록을 요청한다")
    void savePost_recordsPostCreatedEvent() {
        given(accountProfileReader.getProfile(1L)).willReturn(new AccountProfile(1L, "포트닉네임"));
        given(postStore.save(any(Post.class))).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 10L);
            return post;
        });

        postCommandService.savePost(
            "title",
            "content",
            1L,
            List.of()
        );

        ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);
        verify(domainEventRecorder).record(
            eq(EventType.from(PostCreatedEvent.EVENT_TYPE)),
            eq(PostDomainEvent.AGGREGATE_TYPE),
            eq("10"),
            eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().postId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().title()).isEqualTo("title");
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().nickname()).isEqualTo("포트닉네임");
    }

    @Test
    @DisplayName("게시글 삭제 시 같은 트랜잭션 안에서 PostDeleted 이벤트 기록을 요청한다")
    void deletePost_recordsPostDeletedEvent() {
        Post post = Post.builder()
            .title("delete title")
            .content("delete content")
            .accountId(1L)
            .nickname("포트닉네임")
            .build();
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postReader.getById(10L)).willReturn(post);

        postCommandService.deletePost(10L);

        verify(postFileAppender).detachFiles(post);
        verify(viewCountService).deleteViewCount(10L);
        verify(postStore).delete(post);

        ArgumentCaptor<PostDeletedEvent> eventCaptor = ArgumentCaptor.forClass(PostDeletedEvent.class);
        verify(domainEventRecorder).record(
            eq(EventType.from(PostDeletedEvent.EVENT_TYPE)),
            eq(PostDomainEvent.AGGREGATE_TYPE),
            eq("10"),
            eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().postId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().title()).isEqualTo("delete title");
    }
}
