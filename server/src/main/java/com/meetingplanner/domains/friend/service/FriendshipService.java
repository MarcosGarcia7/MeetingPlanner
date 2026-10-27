package com.meetingplanner.domains.friend.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             BlockService blockService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockService = blockService;
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
        FriendshipEntity existing = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b).orElse(null);
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
        try {
            friendshipRepository.saveAndFlush(friendship);
        } catch (DataIntegrityViolationException e) {
            // Both users sent a first request at once; retrying takes the mutual-accept path above.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already sent");
        }

        notifyRequested(target.getId(), requester);
    }

    @Transactional
    public void respond(long responderId, String otherUsername, boolean accept) {
        UserEntity other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pair pair = pair(responderId, other.getId());
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b)
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
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING
                || !friendship.getRequester().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this request");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void unfriend(long userId, String otherUsername) {
        UserEntity other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pair pair = pair(userId, other.getId());
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResourceNotFoundException("Friend not found"));
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void block(long blockerId, long targetUserId) {
        requireUser(blockerId);
        requireUser(targetUserId);

        Pair pair = pair(blockerId, targetUserId);
        OffsetDateTime now = OffsetDateTime.now();
        if (friendshipRepository.insertBlockedIfAbsent(pair.a, pair.b, blockerId, now) == 1) {
            blockService.invalidatePendingBetween(blockerId, targetUserId);
            return;
        }

        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "This friendship changed while blocking; please retry"));
        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            return;
        }
        friendship.setRequester(userRepository.getReferenceById(blockerId));
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setRespondedAt(now);
        friendshipRepository.save(friendship);

        blockService.invalidatePendingBetween(blockerId, targetUserId);
    }

    @Transactional
    public void unblock(long callerId, String targetUsername) {
        UserEntity target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pair pair = pair(callerId, target.getId());
        FriendshipEntity friendship = friendshipRepository.findByUserIdAndFriendIdForUpdate(pair.a, pair.b)
                .filter(f -> f.getStatus() == FriendshipStatus.BLOCKED)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked user not found"));

        if (!friendship.getRequester().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the blocker can unblock this user");
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public CursorPage<FriendshipResponse> listFriends(long userId, String cursor, int size) {
        return listByStatus(userId, FriendshipStatus.ACCEPTED, cursor, size);
    }

    @Transactional(readOnly = true)
    public CursorPage<FriendshipResponse> listIncomingRequests(long userId, String cursor, int size) {
        return listRequests(userId, FriendshipStatus.PENDING, true, cursor, size);
    }

    @Transactional(readOnly = true)
    public CursorPage<FriendshipResponse> listOutgoingRequests(long userId, String cursor, int size) {
        return listRequests(userId, FriendshipStatus.PENDING, false, cursor, size);
    }

    @Transactional(readOnly = true)
    public CursorPage<FriendshipResponse> listBlocked(long userId, String cursor, int size) {
        // Only blocks the caller made; being blocked by someone else stays invisible.
        return listRequests(userId, FriendshipStatus.BLOCKED, false, cursor, size);
    }

    private CursorPage<FriendshipResponse> listByStatus(long userId, FriendshipStatus status, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        Long cursorId = decoded == null ? null : decoded.id();

        List<FriendshipEntity> asUser = friendshipRepository.listByStatusAsUser(
                userId, status, cursorTime, cursorId, PageRequest.of(0, limit + 1));
        List<FriendshipEntity> asFriend = friendshipRepository.listByStatusAsFriend(
                userId, status, cursorTime, cursorId, PageRequest.of(0, limit + 1));
        return toPage(merge(asUser, asFriend, userId), limit, userId);
    }

    // incoming = rows the other user initiated; otherwise rows the caller initiated (requester = caller).
    private CursorPage<FriendshipResponse> listRequests(long userId, FriendshipStatus status, boolean incoming,
                                                        String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        Long cursorId = decoded == null ? null : decoded.id();

        List<FriendshipEntity> asUser = friendshipRepository.listRequestsAsUser(
                userId, status, incoming, cursorTime, cursorId, PageRequest.of(0, limit + 1));
        List<FriendshipEntity> asFriend = friendshipRepository.listRequestsAsFriend(
                userId, status, incoming, cursorTime, cursorId, PageRequest.of(0, limit + 1));
        return toPage(merge(asUser, asFriend, userId), limit, userId);
    }

    private List<FriendshipEntity> merge(List<FriendshipEntity> asUser, List<FriendshipEntity> asFriend, long userId) {
        List<FriendshipEntity> rows = new ArrayList<>(asUser);
        rows.addAll(asFriend);
        rows.sort(Comparator
                .comparing(FriendshipEntity::getCreatedAt).reversed()
                .thenComparing(f -> counterpartId(f, userId), Comparator.reverseOrder()));
        return rows;
    }

    private CursorPage<FriendshipResponse> toPage(List<FriendshipEntity> rows, int size, long userId) {
        boolean hasMore = rows.size() > size;
        List<FriendshipEntity> page = hasMore ? rows.subList(0, size) : rows;
        List<FriendshipResponse> items = page.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getCreatedAt(), counterpartId(page.getLast(), userId))
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private static long counterpartId(FriendshipEntity friendship, long userId) {
        return friendship.getUser().getId() == userId
                ? friendship.getFriend().getId()
                : friendship.getUser().getId();
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