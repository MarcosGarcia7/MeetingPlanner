package com.meetingplanner.domains.friend.service;

import com.meetingplanner.common.api.dto.FriendshipResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Transactional
    public void sendRequest(long requesterId, MemberUsernameRequest request) {
        UserEntity requester = requireUser(requesterId);
        UserEntity target = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot befriend yourself");
        }

        Pair pair = pair(requesterId, target.getId());
        FriendshipEntity existing = friendshipRepository.findByUserIdAndFriendId(pair.a, pair.b).orElse(null);
        if (existing != null) {
            switch (existing.getStatus()) {
                case BLOCKED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Friendship is blocked");
                case ACCEPTED -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
                case PENDING -> {
                    if (!existing.getRequester().getId().equals(requesterId)) {
                        existing.setStatus(FriendshipStatus.ACCEPTED);
                        existing.setRespondedAt(OffsetDateTime.now());
                        friendshipRepository.save(existing);
                        notifyAccepted(requester, existing);
                        return;
                    }
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already sent");
                }
                case DECLINED -> {
                    existing.setRequester(requester);
                    existing.setStatus(FriendshipStatus.PENDING);
                    existing.setRespondedAt(null);
                    friendshipRepository.save(existing);
                    notifyRequested(target.getId(), requester);
                    return;
                }
            }
        }

        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setUser(userRepository.getReferenceById(pair.a));
        friendship.setFriend(userRepository.getReferenceById(pair.b));
        friendship.setRequester(requester);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        notifyRequested(target.getId(), requester);
    }

    @Transactional
    public void respond(long responderId, String otherUsername, boolean accept) {
        UserEntity other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pair pair = pair(responderId, other.getId());
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendId(pair.a, pair.b)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No pending request from this user");
        }
        if (friendship.getRequester().getId().equals(responderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot respond to your own request");
        }

        friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.DECLINED);
        friendship.setRespondedAt(OffsetDateTime.now());
        friendshipRepository.save(friendship);

        if (accept) {
            notifyAccepted(requireUser(responderId), friendship);
        }
    }

    @Transactional
    public void cancel(long requesterId, String otherUsername) {
        UserEntity other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pair pair = pair(requesterId, other.getId());
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendId(pair.a, pair.b)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING
                || !friendship.getRequester().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this request");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void block(long blockerId, long targetUserId) {
        requireUser(blockerId);
        requireUser(targetUserId);

        Pair pair = pair(blockerId, targetUserId);
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendId(pair.a, pair.b).orElse(null);
        if (friendship == null) {
            friendship = new FriendshipEntity();
            friendship.setUser(userRepository.getReferenceById(pair.a));
            friendship.setFriend(userRepository.getReferenceById(pair.b));
        }
        friendship.setRequester(userRepository.getReferenceById(blockerId));
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setRespondedAt(OffsetDateTime.now());
        friendshipRepository.save(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listFriends(long userId) {
        return friendshipRepository.findAcceptedByUserId(userId).stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listIncomingRequests(long userId) {
        return friendshipRepository.findByFriendIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listOutgoingRequests(long userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.PENDING).stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listBlocked(long userId) {
        return friendshipRepository.findBlockedByUserId(userId).stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private void notifyRequested(long targetUserId, UserEntity requester) {
        String link = appBaseUrl + "/friends/requests";
        notificationService.push(targetUserId, "FRIEND_REQUEST", "friend_requests", "FRIEND_REQUEST", Map.of(
                "name", displayName(requireUser(targetUserId)),
                "requester", displayName(requester),
                "link", link));
    }

    private void notifyAccepted(UserEntity acceptor, FriendshipEntity friendship) {
        notificationService.push(friendship.getRequester().getId(), "FRIEND_REQUEST_ACCEPTED",
                "friend_requests", "FRIEND_REQUEST_ACCEPTED", Map.of(
                        "name", displayName(friendship.getRequester()),
                        "acceptor", displayName(acceptor)));
    }

    private UserEntity requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static Pair pair(long a, long b) {
        return a < b ? new Pair(a, b) : new Pair(b, a);
    }

    private static String displayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getUsername();
    }

    private record Pair(long a, long b) {
    }
}