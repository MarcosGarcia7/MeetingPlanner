package com.meetingplanner.domains.notification.controller;

import com.meetingplanner.common.api.dto.NotificationPreferenceUpdateRequest;
import com.meetingplanner.common.api.dto.NotificationPreferencesResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public NotificationPreferencesResponse get(@AuthenticationPrincipal Jwt jwt) {
        return preferenceService.get(CurrentUser.from(jwt).id());
    }

    @PutMapping
    public NotificationPreferencesResponse update(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        return preferenceService.update(CurrentUser.from(jwt).id(), request);
    }
}