package com.careconnect.messaging.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.messaging.dto.MessageDtos.ConversationDto;
import com.careconnect.messaging.dto.MessageDtos.MessageDto;
import com.careconnect.messaging.dto.MessageDtos.SendMessageRequest;
import com.careconnect.messaging.model.Message;
import com.careconnect.messaging.repository.MessageRepository;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.patient.model.Patient;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Secure messaging. Patients may only write to doctors and doctors only to patients,
 * so every message is tied to exactly one patient record.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messages;
    private final AppUserRepository users;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public List<ConversationDto> conversations() {
        Long me = SecurityUtils.currentUser().id();
        Map<Long, List<Message>> byCounterpart = new LinkedHashMap<>();
        for (Message m : messages.findAllForUser(me)) {
            AppUser other = m.getSender().getId().equals(me) ? m.getRecipient() : m.getSender();
            byCounterpart.computeIfAbsent(other.getId(), k -> new ArrayList<>()).add(m);
        }
        List<ConversationDto> result = new ArrayList<>();
        byCounterpart.forEach((otherId, list) -> {
            Message last = list.get(0);   // newest first
            AppUser other = last.getSender().getId().equals(me) ? last.getRecipient() : last.getSender();
            long unread = list.stream().filter(m -> m.getRecipient().getId().equals(me) && m.getReadAt() == null).count();
            result.add(new ConversationDto(other.getId(), other.getFullName(), other.getRole(),
                    last.getPatient().getId(), last.getPatient().getFullName(), last.getSubject(),
                    preview(last.getBody()), last.getCreatedAt(), unread));
        });
        return result;
    }

    /** Messages with one person, oldest first. Opening a thread marks incoming messages as read. */
    @Transactional
    public List<MessageDto> thread(Long counterpartId) {
        Long me = SecurityUtils.currentUser().id();
        messages.markThreadRead(me, counterpartId, LocalDateTime.now());
        List<Message> list = messages.findThread(me, counterpartId);
        if (!list.isEmpty()) {
            AuditContext.setPatientId(list.get(0).getPatient().getId());
        }
        return list.stream().map(m -> toDto(m, me)).toList();
    }

    @Transactional
    public MessageDto send(SendMessageRequest request) {
        AppUser sender = users.require(SecurityUtils.currentUser().id());
        AppUser recipient = users.findById(request.recipientId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> ApiException.notFound("Recipient", request.recipientId()));

        Patient patient;
        if (sender.getRole() == Role.PATIENT && recipient.getRole() == Role.DOCTOR) {
            patient = sender.getPatient();
        } else if (sender.getRole() == Role.DOCTOR && recipient.getRole() == Role.PATIENT) {
            patient = recipient.getPatient();
        } else {
            throw ApiException.badRequest("Messages can only be exchanged between a patient and a doctor.");
        }
        if (patient == null) {
            throw ApiException.badRequest("The patient account is not linked to a patient record.");
        }

        Message message = new Message();
        message.setPatient(patient);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setSubject(request.subject().trim());
        message.setBody(request.body().trim());
        message.setCreatedAt(LocalDateTime.now());
        messages.save(message);
        AuditContext.setPatientId(patient.getId());

        notifications.notifyUser(recipient.getId(), "New message from " + sender.getFullName(),
                request.subject().trim(), NotificationService.TYPE_MESSAGE,
                recipient.getRole() == Role.PATIENT ? "/portal/messages" : "/messages");
        return toDto(message, sender.getId());
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return messages.countByRecipientIdAndReadAtIsNull(SecurityUtils.currentUser().id());
    }

    private static MessageDto toDto(Message m, Long me) {
        return new MessageDto(m.getId(), m.getPatient().getId(), m.getSender().getId(), m.getSender().getFullName(),
                m.getRecipient().getId(), m.getRecipient().getFullName(), m.getSubject(), m.getBody(), m.getReadAt(),
                m.getCreatedAt(), m.getSender().getId().equals(me));
    }

    private static String preview(String body) {
        return body.length() <= 90 ? body : body.substring(0, 90) + "…";
    }
}
