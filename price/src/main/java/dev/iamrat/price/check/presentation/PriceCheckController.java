package dev.iamrat.price.check.presentation;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import dev.iamrat.price.check.application.PriceCheckService;
import dev.iamrat.price.check.presentation.dto.PriceCheckRequest;
import dev.iamrat.price.check.presentation.dto.PriceCheckResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class PriceCheckController {

    private final PriceCheckService priceCheckService;

    @PostMapping("/api/price-checks")
    public ResponseEntity<PriceCheckResponse> checkPrice(@Valid @RequestBody PriceCheckRequest request) {
        return ResponseEntity.ok(PriceCheckResponse.from(priceCheckService.check(request.toCommand())));
    }
}
