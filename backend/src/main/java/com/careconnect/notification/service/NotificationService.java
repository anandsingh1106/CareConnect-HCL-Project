package com.careconnect.notification.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.notification.model.Notification;
import com.careconnect.notification.repository.NotificationRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Creates in-app notifications and serves the current user's inbox (the Angular app polls it). */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_LAB_RESULT = "LAB_RESULT";
    public static final String TYPE_PRESCRIPTION = "PRESCRIPTION";
    public static final String TYPE_LOW_STOCK = "LOW_STOCK";
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_APPOINTMENT = "APPOINTMENT";
    public static final String TYPE_ORDER = "ORDER";

    public record NotificationDto(Long id, String title, String message, String type, String link, boolean read,
                                  LocalDateTime createdAt) {
    }

    private final NotificationRepository notifications;
    private final AppUserRepository users;

    @Transactional
    public void notifyUser(Long userId, String title, String message, String type, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(truncate(title, 200));
        notification.setMessage(truncate(message, 1000));
        notification.setType(type);
        notification.setLink(link);
        notification.setCreatedAt(LocalDateTime.now());
        notifications.save(notification);
    }

    @Transactional
    public void notifyRole(Role role, String title, String message, String type, String link) {
        users.findByRoleAndActiveTrueOrderByFullNameAsc(role)
                .forEach(user -> notifyUser(user.getId(), title, message, type, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> mine(boolean unreadOnly, Pageable pageable) {
        Long userId = SecurityUtils.currentUser().id();
        var page = unreadOnly
                ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notifications.countByUserIdAndReadFalse(SecurityUtils.currentUser().id());
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification", id));
        if (!notification.getUserId().equals(SecurityUtils.currentUser().id())) {
            throw ApiException.notFound("Notification", id);
        }
        notification.setRead(true);
    }

    @Transactional
    public void markAllRead() {
        notifications.markAllRead(SecurityUtils.currentUser().id());
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.getLink(), n.isRead(),
                n.getCreatedAt());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
