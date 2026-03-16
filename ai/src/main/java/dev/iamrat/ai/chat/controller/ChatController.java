package dev.iamrat.ai.chat.controller;

import dev.iamrat.ai.chat.dto.ChatRequest;
import dev.iamrat.ai.chat.dto.ChatResponse;
import dev.iamrat.ai.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String answer = chatService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}