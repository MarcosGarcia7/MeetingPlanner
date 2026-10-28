package com.meetingplanner.domains.user.controller;

import com.meetingplanner.common.api.dto.ChangePasswordRequest;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.DeleteAccountRequest;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.TokenResponse;
import com.meetingplanner.common.api.dto.UserAvailabilityResponse;
import com.meetingplanner.common.api.dto.UserAvailabilityUpdateRequest;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.user.service.AuthService;
import com.meetingplanner.domains.user.service.UserAvailabilityService;
import com.meetingplanner.domains.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserAvailabilityService availabilityService;
    private final MeetingService meetingService;
    private final AuthService authService;

    public UserController(UserService userService, UserAvailabilityService availabilityService,
                          MeetingService meetingService, AuthService authService) {
        this.userService = userService;
        this.availabilityService = availabilityService;
        this.meetingService = meetingService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMe(CurrentUser.from(jwt).id());
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateMe(CurrentUser.from(jwt).id(), request);
    }

    @PostMapping("/me/password")
    public TokenResponse changePassword(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody ChangePasswordRequest request,
                                        HttpServletRequest httpRequest) {
        return authService.changePassword(CurrentUser.from(jwt).id(), request.currentPassword(),
                request.newPassword(), resolveClientIp(httpRequest));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody DeleteAccountRequest request,
                                         HttpServletRequest httpRequest) {
        authService.deleteAccount(CurrentUser.from(jwt).id(), request.currentPassword(),
                resolveClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}")
    public UserProfileResponse getByUsername(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        return userService.getByUsername(CurrentUser.from(jwt).id(), username);
    }

    @GetMapping("/{username}/availability")
    public UserAvailabilityResponse getAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        return availabilityService.getAvailability(CurrentUser.from(jwt).id(), username);
    }

    @GetMapping("/{username}/schedule")
    public CursorPage<MeetingSummaryResponse> getSchedule(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable String username,
                                                          @RequestParam(required = false) String cursor,
                                                          @RequestParam(defaultValue = "20") int size) {
        long viewerId = CurrentUser.from(jwt).id();
        long targetUserId = userService.resolveUserId(username);
        return meetingService.listSchedule(viewerId, targetUserId, cursor, size);
    }

    @GetMapping("/me/availability")
    public UserAvailabilityResponse getAvailability(@AuthenticationPrincipal Jwt jwt) {
        return availabilityService.getAvailability(CurrentUser.from(jwt).id());
    }

    @PutMapping("/me/availability")
    public UserAvailabilityResponse updateAvailability(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody UserAvailabilityUpdateRequest request) {
        return availabilityService.updateAvailability(CurrentUser.from(jwt).id(), request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
