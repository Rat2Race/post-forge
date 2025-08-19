package rat.boardservice.posts.dto.article;

import rat.boardservice.posts.domain.Article;

public record ArticleResponse (
	Long id,
	String title,
	String content,
	String author,
	Long views
) {
	public static ArticleResponse toEntity(Article article) {
		return new ArticleResponse (
				article.getId(),
				article.getTitle(),
				article.getContent(),
				article.getAuthor(),
				article.getViews()
		);
	}
}