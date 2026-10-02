package dev.iamrat.board.post.domain;

import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Modifying
    @Query("UPDATE Post p SET p.views = :views WHERE p.id = :id")
    void updateViews(@Param("id") Long id, @Param("views") long views);

    // 읽은 값을 다시 쓰지 않고 한 문장으로 더한다. 동시 요청은 행 잠금 순서대로 서로의 결과 위에 더한다.
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
    void addLikeCount(@Param("id") Long id, @Param("delta") long delta);

    // 제목이나 본문에 검색어가 들어간 글을 대소문자 구분 없이 찾는다. 검색어가 비면 전체다.
    default Page<Post> findByKeyword(String keyword, Pageable pageable) {
        return findAll(keywordFilter(keyword), pageable);
    }

    private static Specification<Post> keywordFilter(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern)
            );
        };
    }
}
