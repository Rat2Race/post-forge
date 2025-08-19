package com.postforge.auth.article.controller;

import com.postforge.auth.article.domain.dto.ArticleResponse;
import com.postforge.auth.article.domain.entity.Article;
import com.postforge.auth.article.service.BlogService;
import com.postforge.auth.article.service.dto.ServiceDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

//@Controller
@RequiredArgsConstructor
@RequestMapping("/articles")
public class BlogController {

    private final BlogService blogService;

    @GetMapping()
    public String getArticles(Model model) {
        List<ServiceDto> serviceDtoList = blogService.findAll();
        List<ArticleResponse> responseList = serviceDtoList.stream()
                .map(serviceDto -> new ArticleResponse(serviceDto))
                .toList();

        model.addAttribute("articles", responseList);

        return "articleList";
    }

    @GetMapping("/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        ServiceDto serviceDto = blogService.findById(id);
        ArticleResponse response = new ArticleResponse(serviceDto);

        model.addAttribute("article", response);

        return "article";
    }

    @GetMapping("/add")
    public String newArticle(Model model) {
        model.addAttribute("article", new Article());
        return "addArticle";
    }
}

