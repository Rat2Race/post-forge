package rat.boardservice.posts.service;

import rat.boardservice.posts.dto.article.ArticleResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ViewsEvent extends ApplicationEvent {

	private final ArticleResponse articleResponse;

	public ViewsEvent(ArticleResponse articleResponse) {
		super(articleResponse);
		this.articleResponse = articleResponse;
	}
}