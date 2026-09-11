package dev.iamrat.ai.chat.presentation;

import dev.iamrat.ai.chat.application.ChatService;
import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
        @Valid @RequestBody ChatRequest request,
        @AuthenticationPrincipal UserPrincipal user,
        HttpServletRequest servletRequest
    ) {
        String answer = chatService.chat(
            request.message(),
            user.getAccountId(),
            servletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
