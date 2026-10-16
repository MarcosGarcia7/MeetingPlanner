package com.meetingplanner.domains.user.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.UserAvailabilityResponse;
import com.meetingplanner.common.api.dto.UserAvailabilityUpdateRequest;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.user.service.UserAvailabilityService;
import com.meetingplanner.domains.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    public UserController(UserService userService, UserAvailabilityService availabilityService,
                          MeetingService meetingService) {
        this.userService = userService;
        this.availabilityService = availabilityService;
        this.meetingService = meetingService;
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteMe(CurrentUser.from(jwt).id());
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
}