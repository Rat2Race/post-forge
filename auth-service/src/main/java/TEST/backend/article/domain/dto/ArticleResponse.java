package TEST.backend.article.domain.dto;

import TEST.backend.article.service.dto.ServiceDto;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ArticleResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final LocalDateTime createAt;

    public ArticleResponse(ServiceDto article) {
        id = article.getId();
        title = article.getTitle();
        content = article.getContent();
        createAt = article.getCreateAt();
    }
}
