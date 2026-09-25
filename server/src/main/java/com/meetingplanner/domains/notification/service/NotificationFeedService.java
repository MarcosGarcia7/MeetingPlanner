package com.meetingplanner.domains.notification.service;

import com.meetingplanner.common.api.dto.NotificationResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.notification.model.NotificationEntity;
import com.meetingplanner.domains.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationFeedService {

    private final NotificationRepository notificationRepository;

    public NotificationFeedService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(long userId, int limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(long userId, long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setReadAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(long userId) {
        notificationRepository.markAllRead(userId, OffsetDateTime.now());
    }
}