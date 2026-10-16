package com.meetingplanner.domains.notification.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
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
    public CursorPage<NotificationResponse> list(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<NotificationEntity> rows = notificationRepository.listForUser(
                userId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<NotificationEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<NotificationResponse> items = page.stream()
                .map(NotificationResponse::from)
                .toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getCreatedAt(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
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