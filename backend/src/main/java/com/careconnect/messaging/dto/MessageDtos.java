package com.careconnect.messaging.dto;

import com.careconnect.user.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class MessageDtos {

    private MessageDtos() {
    }

    public record MessageDto(Long id, Long patientId, Long senderId, String senderName, Long recipientId,
                             String recipientName, String subject, String body, LocalDateTime readAt,
                             LocalDateTime createdAt, boolean mine) {
    }

    /** One conversation in the inbox: the other person, the latest message and how many are unread. */
    public record ConversationDto(Long counterpartId, String counterpartName, Role counterpartRole,
                                  Long patientId, String patientName, String lastSubject, String lastPreview,
                                  LocalDateTime lastAt, long unread) {
    }

    public record SendMessageRequest(
            @NotNull Long recipientId,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String body) {
    }
}
