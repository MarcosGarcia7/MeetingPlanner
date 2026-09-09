package com.meetingplanner.domains.user.controller;

import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.api.security.CurrentUser;
import com.meetingplanner.domains.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
}