package com.careconnect.messaging.web;

import com.careconnect.messaging.dto.MessageDtos.ConversationDto;
import com.careconnect.messaging.dto.MessageDtos.MessageDto;
import com.careconnect.messaging.dto.MessageDtos.SendMessageRequest;
import com.careconnect.messaging.service.MessageService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Secure messaging")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAuthorize(Authz.MESSAGING)
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "My conversations (one per doctor or patient), newest first")
    @GetMapping("/conversations")
    public List<ConversationDto> conversations() {
        return messageService.conversations();
    }

    @Operation(summary = "Messages exchanged with one user (marks them read)")
    @GetMapping("/with/{userId}")
    public List<MessageDto> thread(@PathVariable Long userId) {
        return messageService.thread(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@Valid @RequestBody SendMessageRequest request) {
        return messageService.send(request);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", messageService.unreadCount());
    }
}
