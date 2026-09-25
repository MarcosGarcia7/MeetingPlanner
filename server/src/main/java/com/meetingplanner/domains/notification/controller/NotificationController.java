package com.meetingplanner.domains.notification.controller;

import com.meetingplanner.common.api.dto.NotificationResponse;
import com.meetingplanner.common.api.security.CurrentUser;
import com.meetingplanner.domains.notification.service.NotificationFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me/notifications")
public class NotificationController {

    private final NotificationFeedService notificationFeedService;

    public NotificationController(NotificationFeedService notificationFeedService) {
        this.notificationFeedService = notificationFeedService;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "30") int limit) {
        return notificationFeedService.list(CurrentUser.from(jwt).id(), limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unread", notificationFeedService.unreadCount(CurrentUser.from(jwt).id()));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable long notificationId) {
        notificationFeedService.markRead(CurrentUser.from(jwt).id(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationFeedService.markAllRead(CurrentUser.from(jwt).id());
        return ResponseEntity.noContent().build();
    }
}