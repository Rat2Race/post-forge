package dev.iamrat.board.post.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import dev.iamrat.board.post.domain.PostReferenceLink;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostReferenceLinkPersistenceAdapterTest {

    @Mock
    private PostReferenceLinkRepository repository;

    @InjectMocks
    private PostReferenceLinkPersistenceAdapter adapter;

    @Test
    @DisplayName("참조 링크 저장과 조회를 repository에 위임한다")
    void delegatesReferenceLinkPersistence() {
        PostReferenceLink link = PostReferenceLink.builder().id(1L).canonicalUrl("https://news.example/a").build();
        given(repository.save(link)).willReturn(link);
        given(repository.existsByCanonicalUrl("https://news.example/a")).willReturn(true);
        given(repository.countByKeywordAndProductIdBetween(
            "galaxybook",
            10L,
            LocalDateTime.of(2026, 6, 21, 0, 0),
            LocalDateTime.of(2026, 6, 22, 0, 0)
        )).willReturn(2L);
        given(repository.findByPost_Id(10L)).willReturn(List.of(link));
        given(repository.findByPost_IdInOrderByPost_IdAscIdAsc(List.of(10L))).willReturn(List.of(link));

        assertThat(adapter.save(link)).isSameAs(link);
        assertThat(adapter.existsByCanonicalUrl("https://news.example/a")).isTrue();
        assertThat(adapter.countByKeywordAndProductIdOnDate("GalaxyBook", 10L, LocalDate.of(2026, 6, 21)))
            .isEqualTo(2L);
        assertThat(adapter.findByPostId(10L)).containsExactly(link);
        assertThat(adapter.findByPostIds(List.of(10L))).containsExactly(link);
        assertThat(adapter.findByPostIds(List.of())).isEmpty();
    }
}
