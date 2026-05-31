package dev.iamrat.catalog.product.presentation;

import dev.iamrat.catalog.product.application.ProductService;
import dev.iamrat.catalog.product.presentation.dto.ProductCategoryResponse;
import dev.iamrat.catalog.product.presentation.dto.ProductResponse;
import dev.iamrat.catalog.product.presentation.dto.ProductUpsertRequest;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/products")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.findActive(pageable).map(ProductResponse::from);
        return ResponseEntity.ok(PageResponse.from(products));
    }

    @GetMapping("/api/products/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
        @RequestParam String query,
        @RequestParam(required = false) Long categoryId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.search(query, categoryId, pageable)
            .map(ProductResponse::from);
        return ResponseEntity.ok(PageResponse.from(products));
    }

    @GetMapping("/api/products/{productId:\\d+}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ProductResponse.from(productService.getById(productId)));
    }

    @GetMapping("/api/products/categories")
    public ResponseEntity<List<ProductCategoryResponse>> getCategories() {
        List<ProductCategoryResponse> categories = productService.findActiveCategories().stream()
            .map(ProductCategoryResponse::from)
            .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/api/products/categories/{categoryId:\\d+}")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByCategory(
        @PathVariable Long categoryId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.findActiveByCategory(categoryId, pageable)
            .map(ProductResponse::from);
        return ResponseEntity.ok(PageResponse.from(products));
    }

    @PostMapping("/api/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> upsertProduct(@RequestBody @Valid ProductUpsertRequest request) {
        ProductResponse response = ProductResponse.from(productService.upsert(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/admin/products/{productId:\\d+}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> hideProduct(@PathVariable Long productId) {
        productService.hide(productId);
        return ResponseEntity.ok(MessageResponse.of("상품 숨김 처리 완료"));
    }
}
