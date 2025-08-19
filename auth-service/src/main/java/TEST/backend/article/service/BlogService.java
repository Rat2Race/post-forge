package TEST.backend.article.service;

import TEST.backend.article.domain.dto.ArticleRequest;
import TEST.backend.article.domain.entity.Article;
import TEST.backend.article.repository.BlogRepository;
import TEST.backend.article.service.dto.ServiceDto;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class BlogService {

    private final BlogRepository blogRepository;

    @Transactional
    public ServiceDto save(ArticleRequest request) {
        Article article = blogRepository.save(request.toEntity());
        ServiceDto serviceDto = ServiceDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .build();

        return serviceDto;
    }

    public List<ServiceDto> findAll() {
        List<Article> articles = blogRepository.findAll();
        List<ServiceDto> serviceDtoList = articles.stream()
                .map(article -> ServiceDto.builder()
                        .id(article.getId())
                        .title(article.getTitle())
                        .content(article.getContent())
                        .build())
                .toList();

        return serviceDtoList;
    }

    public ServiceDto findById(Long id) {
        Optional<Article> article = blogRepository.findById(id);
        ServiceDto serviceDto = ServiceDto.builder()
                .id(article.get().getId())
                .title(article.get().getTitle())
                .content(article.get().getContent())
                .build();

        return serviceDto;
    }

    @Transactional
    public void delete(Long id) {
        Optional<Article> article = blogRepository.findById(id);
        blogRepository.delete(article.get());
    }

    @Transactional
    public void update(Long id, ArticleRequest request) {
        Optional<Article> savedArticle = blogRepository.findById(id);
        savedArticle.get().update(request.toEntity());
    }
}
