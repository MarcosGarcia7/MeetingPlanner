package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.api.exceptions.EmailTakenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.AccountTokenType;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileAuthorizationService authorization;
    private final AccountTokenService accountTokenService;
    private final NotificationService notificationService;
    private final JtiStorageService jtiStorageService;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileAuthorizationService authorization,
                           AccountTokenService accountTokenService,
                           NotificationService notificationService,
                           JtiStorageService jtiStorageService) {
        this.userRepository = userRepository;
        this.authorization = authorization;
        this.accountTokenService = accountTokenService;
        this.notificationService = notificationService;
        this.jtiStorageService = jtiStorageService;
    }

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMe(long userId) {
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateMe(long userId, UserProfileUpdateRequest request) {
        UserEntity user = requireUser(userId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.avatarColor() != null) {
            user.setAvatarColor(request.avatarColor());
        }
        if (request.isPrivate() != null) {
            user.setPrivate(request.isPrivate());
        }
        if (request.friendsCanViewPrivate() != null) {
            user.setFriendsCanViewPrivate(request.friendsCanViewPrivate());
        }
        if (request.pendingEmail() != null) {
            updatePendingEmail(user, request.pendingEmail());
        }

        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        return toProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(long viewerId, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!authorization.canViewProfile(viewerId, user.getId())) {
            throw new ResourceNotFoundException("User not found");
        }
        return toProfile(user);
    }

    @Override
    @Transactional
    public void deleteMe(long userId) {
        UserEntity user = requireUser(userId);
        jtiStorageService.revokeAllJtis(user.getId().toString());
        userRepository.delete(user);
    }

    private void updatePendingEmail(UserEntity user, String pendingEmail) {
        String normalized = pendingEmail.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank() || normalized.equalsIgnoreCase(user.getEmail())) {
            user.setPendingEmail(null);
            return;
        }

        if (userRepository.existsByEmail(normalized)) {
            throw new EmailTakenException(normalized);
        }

        user.setPendingEmail(normalized);
        String token = accountTokenService.create(user, AccountTokenType.EMAIL_CHANGE, normalized, null, null);
        String link = appBaseUrl + "/verify-email?token=" + token;
        notificationService.queueSecurityEmail(user, "EMAIL_CHANGE", Map.of(
                "name", displayName(user),
                "link", link));
    }

    private UserEntity requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String displayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getUsername();
    }

    private UserProfileResponse toProfile(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarColor(),
                user.isPrivate(),
                user.isFriendsCanViewPrivate(),
                user.getEmailVerifiedAt() != null
        );
    }
}