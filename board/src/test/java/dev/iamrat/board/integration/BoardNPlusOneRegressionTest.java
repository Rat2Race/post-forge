package dev.iamrat.board.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.infrastructure.persistence.CommentRepository;
import dev.iamrat.board.comment.presentation.CommentDetailResponse;
import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.file.infrastructure.persistence.FileRepository;
import dev.iamrat.board.integration.security.WithMockAccount;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.infrastructure.persistence.PostRepository;
import dev.iamrat.board.post.presentation.PostDetailResponse;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfileManager;
import dev.iamrat.core.account.AccountProfileReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.default_batch_fetch_size=1000"
})
@ActiveProfiles("test")
@Transactional
class BoardNPlusOneRegressionTest {

    @Autowired
    private PostQueryService postQueryService;

    @Autowired
    private CommentQueryService commentQueryService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private ViewCountService viewCountService;

    @MockitoBean
    private AccountProfileReader accountProfileReader;

    @MockitoBean
    private AccountProfileManager accountProfileManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        given(viewCountService.getViewCounts(anyList())).willAnswer(invocation -> {
            List<Long> postIds = invocation.getArgument(0);
            return postIds.stream().collect(Collectors.toMap(Function.identity(), ignored -> 0L));
        });
    }

    @Test
    @WithMockAccount
    @DisplayName("게시글 목록 조회는 DTO 변환까지 데이터 수에 비례해 쿼리가 증가하지 않는다")
    void getPosts_doesNotIntroduceNPlusOneQueries() {
        seedPosts(20);

        long smallCount = countQueries(() -> {
            Page<PostDetailResponse> responses = postQueryService.getPosts(null, pageable(1), null);
            assertThat(responses.getContent()).hasSize(1);
        });
        long largeCount = countQueries(() -> {
            Page<PostDetailResponse> responses = postQueryService.getPosts(null, pageable(20), null);
            assertThat(responses.getContent()).hasSize(20);
        });

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 2);
    }

    @Test
    @WithMockAccount
    @DisplayName("댓글 목록 조회는 reply count DTO 변환까지 데이터 수에 비례해 쿼리가 증가하지 않는다")
    void getCommentsByPost_doesNotIntroduceNPlusOneQueries() {
        Post post = postRepository.save(post(0));
        seedComments(post, 20);

        long smallCount = countQueries(() -> {
            Page<CommentDetailResponse> responses = commentQueryService.getCommentsByPost(post.getId(), pageable(1), null);
            assertThat(responses.getContent()).hasSize(1);
        });
        long largeCount = countQueries(() -> {
            Page<CommentDetailResponse> responses = commentQueryService.getCommentsByPost(post.getId(), pageable(20), null);
            assertThat(responses.getContent()).hasSize(20);
        });

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 2);
    }

    private long countQueries(Runnable action) {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        action.run();

        return statistics.getPrepareStatementCount();
    }

    private void seedPosts(int count) {
        for (int index = 0; index < count; index++) {
            Post post = postRepository.save(post(index));
            commentRepository.save(Comment.create(post, null, "댓글 " + index, 2L, "commenter"));
            fileRepository.save(file(post, index));
        }
        postRepository.flush();
    }

    private void seedComments(Post post, int count) {
        for (int index = 0; index < count; index++) {
            commentRepository.save(Comment.create(post, null, "댓글 " + index, 2L, "commenter"));
        }
        commentRepository.flush();
    }

    private Post post(int index) {
        return Post.create("게시글 " + index, "게시글 본문입니다 " + index, List.of("tag-" + index), 1L, "writer");
    }

    private PostFile file(Post post, int index) {
        return PostFile.builder()
            .originalFileName("file-" + index + ".png")
            .savedFileName("saved-" + index + ".png")
            .filePath("/tmp/file-" + index + ".png")
            .fileSize(100L + index)
            .fileType("image/png")
            .post(post)
            .build();
    }

    private PageRequest pageable(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
