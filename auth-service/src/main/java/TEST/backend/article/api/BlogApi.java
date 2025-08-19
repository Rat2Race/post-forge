package TEST.backend.article.api;

import TEST.backend.article.domain.dto.ArticleRequest;
import TEST.backend.article.domain.dto.ArticleResponse;
import TEST.backend.article.service.BlogService;
import TEST.backend.article.service.dto.ServiceDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class BlogApi {

    private final BlogService blogService;

    //request form make
    @PostMapping("/add")
    public ResponseEntity<ArticleResponse> newArticle(@Valid @RequestBody ArticleRequest request) {
        ServiceDto serviceDto = blogService.save(request);
        ArticleResponse response = new ArticleResponse(serviceDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //request give article List
    @GetMapping()
    public ResponseEntity<List<ArticleResponse>> findAllArticles() {
        List<ServiceDto> serviceDtoList = blogService.findAll();
        List<ArticleResponse> responseList = serviceDtoList.stream()
                .map(serviceDto -> new ArticleResponse(serviceDto))
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> findArticle(@PathVariable Long id) {
        ServiceDto serviceDto = blogService.findById(id);
        ArticleResponse response = new ArticleResponse(serviceDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        blogService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        blogService.update(id, request);
        return ResponseEntity.ok().build();
    }
}
