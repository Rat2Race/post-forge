package dev.iamrat.board.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.application.PostStore;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.infrastructure.persistence.PostPersistenceAdapter;
import dev.iamrat.board.view.application.ViewCountStore;
import dev.iamrat.board.view.application.ViewCountSyncScheduler;
import dev.iamrat.core.account.AccountProfileManager;
import dev.iamrat.core.account.AccountProfileReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 조회수 동기화가 DB에 커밋된 글만 dirty 집합에서 지우는지 실제 트랜잭션으로 확인한다. */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Import(ViewCountSyncTransactionTest.FailableUpdates.class)
class ViewCountSyncTransactionTest {

    private static final String PROCESSING_KEY = "post:views:dirty:processing";

    @Autowired
    private ViewCountSyncScheduler viewCountSyncScheduler;

    @Autowired
    private PostStore postStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ViewCountStore viewCountStore;

    @MockitoBean
    private AccountProfileReader accountProfileReader;

    @MockitoBean
    private AccountProfileManager accountProfileManager;

    private final List<Long> postIds = new ArrayList<>();

    @AfterEach
    void deletePosts() {
        postIds.forEach(id -> jdbcTemplate.update("DELETE FROM posts WHERE id = ?", id));
    }

    @Test
    @DisplayName("한 글의 DB 반영이 실패해도 나머지는 커밋되고, 커밋된 글만 dirty 집합에서 지운다")
    void syncRemovesOnlyCommittedIdsFromDirtySet() {
        Long synced = createPost("동기화 성공");
        Long failing = createPost("동기화 실패");
        given(viewCountStore.claimDirtyIdsForProcessing()).willReturn(Optional.of(PROCESSING_KEY));
        given(viewCountStore.findDirtyIds(PROCESSING_KEY))
            .willReturn(new LinkedHashSet<>(List.of(synced.toString(), failing.toString())));
        given(viewCountStore.findViewCount(synced)).willReturn(Optional.of(42L));
        given(viewCountStore.findViewCount(failing)).willReturn(Optional.of(7L));
        // 행 잠금 대기 시간 초과처럼 한 글의 갱신만 실패시킨다.
        doThrow(new CannotAcquireLockException("lock timeout")).when(postStore).updateViews(eq(failing), anyLong());

        viewCountSyncScheduler.syncViewCountsToDb();

        assertThat(jdbcTemplate.queryForObject("SELECT views FROM posts WHERE id = ?", Long.class, synced)).isEqualTo(42L);
        verify(viewCountStore).removeProcessedDirtyIds(PROCESSING_KEY, Set.of(synced.toString()));
    }

    private Long createPost(String title) {
        Long id = postStore.save(Post.create(title, "본문", null, 1L, "writer")).getId();
        postIds.add(id);
        return id;
    }

    @TestConfiguration
    static class FailableUpdates {

        // 실제 저장소에 위임하되, 테스트가 특정 글의 갱신만 실패시킬 수 있게 한다.
        @Bean
        @Primary
        PostStore failablePostStore(PostPersistenceAdapter adapter) {
            return mock(PostStore.class, AdditionalAnswers.delegatesTo(adapter));
        }
    }
}
