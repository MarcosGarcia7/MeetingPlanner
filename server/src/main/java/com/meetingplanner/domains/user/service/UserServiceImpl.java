package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.SearchCursorCodec;
import com.meetingplanner.common.api.SearchQuery;
import com.meetingplanner.common.api.Timezones;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.api.dto.UserSearchResult;
import com.meetingplanner.common.api.exceptions.EmailTakenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.UserRelationship;
import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.AccountTokenType;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final AccountTokenService accountTokenService;
    private final NotificationService notificationService;
    private final JtiStorageService jtiStorageService;

    public UserServiceImpl(UserRepository userRepository,
                           FriendshipRepository friendshipRepository,
                           AccountTokenService accountTokenService,
                           NotificationService notificationService,
                           JtiStorageService jtiStorageService) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.accountTokenService = accountTokenService;
        this.notificationService = notificationService;
        this.jtiStorageService = jtiStorageService;
    }

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMe(long userId) {
        return toProfile(requireUser(userId), true);
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
        if (request.timezone() != null) {
            user.setTimezone(Timezones.resolve(request.timezone()));
        }
        if (request.isPrivate() != null) {
            user.setPrivate(request.isPrivate());
        }
        if (request.friendsCanViewSchedule() != null) {
            user.setFriendsCanViewSchedule(request.friendsCanViewSchedule());
        }
        if (request.pendingEmail() != null) {
            updatePendingEmail(user, request.pendingEmail());
        }

        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        return toProfile(user, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(long viewerId, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfile(user, viewerId == user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSearchResult> search(long userId, String query, String cursor, int size) {
        SearchQuery search = SearchQuery.parse(query);
        int limit = CursorCodec.clampSize(size);
        SearchCursorCodec.Cursor decoded = SearchCursorCodec.decode(cursor);
        List<UserEntity> rows = userRepository.search(
                userId,
                search.pattern(),
                decoded == null ? null : decoded.value(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<UserEntity> page = hasMore ? rows.subList(0, limit) : rows;
        Map<Long, UserRelationship> relationships = relationshipsWith(userId, page);
        List<UserSearchResult> items = page.stream()
                .map(user -> toSearchResult(user, relationships.getOrDefault(user.getId(), UserRelationship.NONE)))
                .toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? SearchCursorCodec.encode(page.getLast().getUsername().toLowerCase(Locale.ROOT), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
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

    private Map<Long, UserRelationship> relationshipsWith(long userId, List<UserEntity> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = users.stream().map(UserEntity::getId).toList();
        Map<Long, UserRelationship> relationships = new HashMap<>();
        for (FriendshipEntity friendship : friendshipRepository.findBetweenUserAnd(userId, ids)) {
            long otherId = friendship.getUser().getId() == userId
                    ? friendship.getFriend().getId()
                    : friendship.getUser().getId();
            // DECLINED maps to NONE: either side may send a fresh request.
            UserRelationship relationship = switch (friendship.getStatus()) {
                case ACCEPTED -> UserRelationship.FRIENDS;
                case PENDING -> friendship.getRequester().getId() == userId
                        ? UserRelationship.REQUEST_SENT
                        : UserRelationship.REQUEST_RECEIVED;
                case DECLINED, BLOCKED -> UserRelationship.NONE;
            };
            relationships.put(otherId, relationship);
        }
        return relationships;
    }

    private static UserSearchResult toSearchResult(UserEntity user, UserRelationship relationship) {
        return new UserSearchResult(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarColor(),
                relationship);
    }

    private UserProfileResponse toProfile(UserEntity user, boolean includeEmail) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarColor(),
                user.getTimezone(),
                user.isPrivate(),
                user.isFriendsCanViewSchedule(),
                user.getEmailVerifiedAt() != null
        );
    }
}
