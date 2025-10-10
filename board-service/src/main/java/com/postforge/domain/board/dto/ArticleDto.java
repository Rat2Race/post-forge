package com.postforge.domain.board.dto;

import com.postforge.domain.board.entity.Article;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ArticleDto(
    Long id,
    String title,
    String content,
    LocalDateTime createAt,
    String createBy,
    LocalDateTime modifiedAt,
    String modifiedBy
) {

    public static ArticleDto from(Article entity) {
        return ArticleDto.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .content(entity.getContent())
            .build();
    }

    public Article toEntity() {
        return Article.builder()
            .id(this.id)
            .title(this.title)
            .content(this.content)
            .build();
    }
}
