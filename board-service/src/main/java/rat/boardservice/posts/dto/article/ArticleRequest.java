package rat.boardservice.posts.dto.article;

public record ArticleRequest (
		String title,
		String content,
		String author
) {
}