package com.zosh.controller;

import com.zosh.modal.User;
import com.zosh.payload.request.ChatHelpRequest;
import com.zosh.payload.response.ChatHelpResponse;
import com.zosh.payload.response.ChatStatusResponse;
import com.zosh.service.ChatHelpService;
import com.zosh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatHelpController {

    private final ChatHelpService chatHelpService;
    private final UserService userService;

    @PostMapping("/help")
    public ResponseEntity<ChatHelpResponse> getAiHelp(
            @Valid @RequestBody ChatHelpRequest request,
            @RequestHeader(value = "Authorization", required = false) String jwt
    ) {
        User user = null;
        if (StringUtils.hasText(jwt)) {
            try {
                user = userService.getUserFromJwtToken(jwt);
            } catch (Exception ex) {
                log.debug("JWT validation bypassed or failed for optional help chat: {}", ex.getMessage());
            }
        }
        ChatHelpResponse response = chatHelpService.getAiHelp(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<ChatStatusResponse> getChatStatus() {
        return ResponseEntity.ok(chatHelpService.getChatStatus());
    }
}
