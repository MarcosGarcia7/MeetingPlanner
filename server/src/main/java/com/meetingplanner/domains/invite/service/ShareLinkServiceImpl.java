package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.CreateGroupShareLinkRequest;
import com.meetingplanner.common.api.dto.CreateMeetingShareLinkRequest;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.dto.ShareLinkJoinResponse;
import com.meetingplanner.common.api.dto.ShareLinkPreviewResponse;
import com.meetingplanner.common.api.dto.ShareLinkResponse;
import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.common.api.exceptions.RateLimitExceededException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.ShareLinkJoinMode;
import com.meetingplanner.common.api.model.ShareLinkJoinOutcome;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupShareLinkEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.repository.GroupShareLinkRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.model.MeetingShareLinkEntity;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.repository.MeetingShareLinkRepository;
import com.meetingplanner.domains.meeting.service.MeetingAuthorizationService;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.request.service.JoinRequestService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class ShareLinkServiceImpl implements ShareLinkService {

    static final String GUEST_IP_PREFIX = "share-guest:ip:";
    static final String GUEST_EMAIL_PREFIX = "share-guest:email:";
    static final int GUEST_IP_MAX = 10;
    static final long GUEST_IP_WINDOW_SECONDS = 3600;
    static final long GUEST_EMAIL_WINDOW_SECONDS = 600;

    private static final String INVALID_LINK = "This link is invalid or has been disabled";

    private final GroupShareLinkRepository groupShareLinkRepository;
    private final MeetingShareLinkRepository meetingShareLinkRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;
    private final GroupAuthorizationService groupAuthorization;
    private final MeetingAuthorizationService meetingAuthorization;
    private final MeetingConflictService conflictService;
    private final MeetingService meetingService;
    private final JoinRequestService joinRequestService;
    private final BlockService blockService;
    private final LoginAttemptService attemptCounter;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public ShareLinkServiceImpl(GroupShareLinkRepository groupShareLinkRepository,
                                MeetingShareLinkRepository meetingShareLinkRepository,
                                GroupRepository groupRepository,
                                GroupMemberRepository groupMemberRepository,
                                MeetingRepository meetingRepository,
                                MeetingParticipantRepository meetingParticipantRepository,
                                UserRepository userRepository,
                                GroupAuthorizationService groupAuthorization,
                                MeetingAuthorizationService meetingAuthorization,
                                MeetingConflictService conflictService,
                                MeetingService meetingService,
                                JoinRequestService joinRequestService,
                                BlockService blockService,
                                LoginAttemptService attemptCounter) {
        this.groupShareLinkRepository = groupShareLinkRepository;
        this.meetingShareLinkRepository = meetingShareLinkRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.userRepository = userRepository;
        this.groupAuthorization = groupAuthorization;
        this.meetingAuthorization = meetingAuthorization;
        this.conflictService = conflictService;
        this.meetingService = meetingService;
        this.joinRequestService = joinRequestService;
        this.blockService = blockService;
        this.attemptCounter = attemptCounter;
    }

    // ---- Management (group) ----

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareLinkResponse> getGroupLink(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        requireGroupManager(userId, group);
        return groupShareLinkRepository.findByGroupIdAndRevokedAtIsNull(groupId).map(this::toResponse);
    }

    @Override
    @Transactional
    public ShareLinkResponse createGroupLink(long userId, long groupId, CreateGroupShareLinkRequest request) {
        // Locked so concurrent resets serialize on the one-active-link index.
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        requireGroupManager(userId, group);
        UserEntity creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OffsetDateTime now = OffsetDateTime.now();
        groupShareLinkRepository.revokeActive(groupId, now);
        GroupShareLinkEntity link = new GroupShareLinkEntity();
        link.setGroup(group);
        link.setCreatedBy(creator);
        link.setToken(InviteTokens.generateRawToken());
        link.setJoinMode(request.joinMode());
        link.setExpiresAt(request.expiresInDays() == null ? null : now.plusDays(request.expiresInDays()));
        link.setMaxUses(request.maxUses());
        link.setCreatedAt(now);
        groupShareLinkRepository.saveAndFlush(link);
        return toResponse(link);
    }

    @Override
    @Transactional
    public void revokeGroupLink(long userId, long groupId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        requireGroupManager(userId, group);
        groupShareLinkRepository.revokeActive(groupId, OffsetDateTime.now());
    }

    // ---- Management (meeting) ----

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareLinkResponse> getMeetingLink(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        requireMeetingManager(userId, meeting);
        return meetingShareLinkRepository.findByMeetingIdAndRevokedAtIsNull(meetingId).map(this::toResponse);
    }

    @Override
    @Transactional
    public ShareLinkResponse createMeetingLink(long userId, long meetingId, CreateMeetingShareLinkRequest request) {
        MeetingEntity meeting = lockMeetingWithGroup(meetingId);
        requireMeetingManager(userId, meeting);
        if (meeting.hasEnded()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This meeting has already ended");
        }
        UserEntity creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OffsetDateTime now = OffsetDateTime.now();
        meetingShareLinkRepository.revokeActive(meetingId, now);
        MeetingShareLinkEntity link = new MeetingShareLinkEntity();
        link.setMeeting(meeting);
        link.setCreatedBy(creator);
        link.setToken(InviteTokens.generateRawToken());
        link.setExpiresAt(request.expiresInDays() == null ? null : now.plusDays(request.expiresInDays()));
        link.setMaxUses(request.maxUses());
        link.setCreatedAt(now);
        meetingShareLinkRepository.saveAndFlush(link);
        return toResponse(link);
    }

    @Override
    @Transactional
    public void revokeMeetingLink(long userId, long meetingId) {
        MeetingEntity meeting = lockMeetingWithGroup(meetingId);
        requireMeetingManager(userId, meeting);
        meetingShareLinkRepository.revokeActive(meetingId, OffsetDateTime.now());
    }

    // ---- Using a link ----

    @Override
    @Transactional(readOnly = true)
    public ShareLinkPreviewResponse preview(Long viewerId, String token) {
        Optional<GroupShareLinkEntity> groupLink = groupShareLinkRepository.findByToken(token);
        if (groupLink.isPresent()) {
            GroupShareLinkEntity link = groupLink.get();
            GroupEntity group = link.getGroup();
            assertUsable(link, group);
            Boolean viewerIsMember = viewerId == null ? null : isGroupMember(viewerId, group);
            return new ShareLinkPreviewResponse(TargetKind.GROUP, group.getId(), group.getGroupName(),
                    null, null, null, null, null,
                    link.getCreatedBy().getUsername(), effectiveMode(link, group), viewerIsMember);
        }
        MeetingShareLinkEntity link = meetingShareLinkRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException(INVALID_LINK));
        MeetingEntity meeting = link.getMeeting();
        assertUsable(link, meeting);
        Boolean viewerIsMember = viewerId == null ? null : meetingAuthorization.isParticipantOrCreator(viewerId, meeting);
        GroupEntity group = meeting.getGroup();
        return new ShareLinkPreviewResponse(TargetKind.MEETING,
                group == null ? null : group.getId(), group == null ? null : group.getGroupName(),
                meeting.getId(), meeting.getTitle(), meeting.getStartTime(), meeting.getEndTime(),
                meeting.getLocation(), link.getCreatedBy().getUsername(), ShareLinkJoinMode.DIRECT, viewerIsMember);
    }

    @Override
    @Transactional
    public ShareLinkJoinResponse join(long userId, String token) {
        Optional<GroupShareLinkEntity> groupLink = groupShareLinkRepository.findByToken(token);
        if (groupLink.isPresent()) {
            return joinGroup(userId, groupLink.get());
        }
        MeetingShareLinkEntity link = meetingShareLinkRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException(INVALID_LINK));
        return joinMeeting(userId, link);
    }

    private ShareLinkJoinResponse joinGroup(long userId, GroupShareLinkEntity link) {
        // Lock order: group -> link (the use-count update).
        GroupEntity group = groupRepository.findByIdForUpdate(link.getGroup().getId())
                .orElseThrow(() -> new InvalidTokenException(INVALID_LINK));
        assertUsable(link, group);
        long groupId = group.getId();
        if (isGroupMember(userId, group)) {
            return new ShareLinkJoinResponse(ShareLinkJoinOutcome.ALREADY_MEMBER, TargetKind.GROUP, groupId, null, null);
        }
        if (groupAuthorization.isBanned(userId, group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this group");
        }
        blockService.assertNotBlocked(userId, link.getCreatedBy().getId(), "You cannot join with this link");

        if (effectiveMode(link, group) == ShareLinkJoinMode.REQUEST) {
            JoinRequestResponse request = joinRequestService.requestGroupJoin(userId, groupId);
            consume(link);
            return new ShareLinkJoinResponse(ShareLinkJoinOutcome.REQUESTED, TargetKind.GROUP, groupId, null, request.id());
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // The link stands in for an invite, so a private group doesn't stop a direct join.
        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(group);
        member.setUser(user);
        member.setAdmin(false);
        groupMemberRepository.save(member);
        consume(link);
        return new ShareLinkJoinResponse(ShareLinkJoinOutcome.JOINED, TargetKind.GROUP, groupId, null, null);
    }

    private ShareLinkJoinResponse joinMeeting(long userId, MeetingShareLinkEntity link) {
        // Lock order: group -> meeting -> link (the use-count update).
        MeetingEntity meeting = lockMeetingWithGroup(link.getMeeting().getId());
        assertUsable(link, meeting);
        long meetingId = meeting.getId();
        Long groupId = meeting.getGroup() == null ? null : meeting.getGroup().getId();
        if (meetingAuthorization.isParticipantOrCreator(userId, meeting)) {
            return new ShareLinkJoinResponse(ShareLinkJoinOutcome.ALREADY_MEMBER, TargetKind.MEETING, groupId, meetingId, null);
        }
        if (meeting.getGroup() != null && groupAuthorization.isBanned(userId, meeting.getGroup())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot join this meeting");
        }
        blockService.assertNotBlocked(userId, link.getCreatedBy().getId(), "You cannot join with this link");
        boolean conflicts = !conflictService.findScheduleConflicts(
                userId, meeting.getStartTime(), meeting.getEndTime(), meetingId).isEmpty();
        if (conflicts) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This meeting conflicts with your schedule");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        MeetingParticipantEntity participant = new MeetingParticipantEntity();
        participant.setMeeting(meeting);
        participant.setUser(user);
        try {
            meetingParticipantRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException e) {
            // Safety net only; the meeting lock already serializes participant inserts.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a participant of this meeting");
        }
        consume(link);
        return new ShareLinkJoinResponse(ShareLinkJoinOutcome.JOINED, TargetKind.MEETING, groupId, meetingId, null);
    }

    @Override
    @Transactional
    public void requestGuestInvite(String token, String email, String clientIp) {
        MeetingShareLinkEntity link = meetingShareLinkRepository.findByToken(token).orElse(null);
        if (link == null) {
            if (groupShareLinkRepository.findByToken(token).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sign in to join this group");
            }
            throw new InvalidTokenException(INVALID_LINK);
        }
        MeetingEntity meeting = link.getMeeting();
        assertUsable(link, meeting);

        String trimmed = email.trim();
        rateLimit(GUEST_IP_PREFIX + clientIp, GUEST_IP_MAX, GUEST_IP_WINDOW_SECONDS);
        rateLimit(GUEST_EMAIL_PREFIX + link.getId() + ":" + trimmed.toLowerCase(Locale.ROOT), 1, GUEST_EMAIL_WINDOW_SECONDS);

        // Invite first (it locks group -> meeting), then count the use, keeping the link last in lock order.
        // The outcome is deliberately ignored: the answer must not reveal who is invited or has an account.
        meetingService.inviteFromShareLink(link.getCreatedBy().getId(), meeting.getId(), trimmed);
        consume(link);
    }

    // ---- Helpers ----

    private void requireGroupManager(long userId, GroupEntity group) {
        if (!groupAuthorization.isAdminOrOwner(userId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
    }

    private void requireMeetingManager(long userId, MeetingEntity meeting) {
        if (!meetingAuthorization.canManage(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
    }

    private MeetingEntity lockMeetingWithGroup(long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (meeting.getGroup() != null) {
            groupRepository.findByIdForUpdate(meeting.getGroup().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        }
        return meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    private boolean isGroupMember(long userId, GroupEntity group) {
        return groupAuthorization.isOwner(userId, group) || groupAuthorization.isMember(userId, group);
    }

    // A REQUEST link on a public group joins directly; anyone could join it anyway.
    private static ShareLinkJoinMode effectiveMode(GroupShareLinkEntity link, GroupEntity group) {
        return link.getJoinMode() == ShareLinkJoinMode.REQUEST && group.isPrivate()
                ? ShareLinkJoinMode.REQUEST
                : ShareLinkJoinMode.DIRECT;
    }

    private void assertUsable(GroupShareLinkEntity link, GroupEntity group) {
        assertUsable(link.getRevokedAt(), link.isExpired(OffsetDateTime.now()), link.isExhausted(),
                groupAuthorization.isAdminOrOwner(link.getCreatedBy().getId(), group));
    }

    private void assertUsable(MeetingShareLinkEntity link, MeetingEntity meeting) {
        assertUsable(link.getRevokedAt(), link.isExpired(OffsetDateTime.now()), link.isExhausted(),
                meetingAuthorization.canManage(link.getCreatedBy().getId(), meeting));
        if (meeting.hasEnded()) {
            throw new InvalidTokenException("This meeting has already ended");
        }
    }

    // The creator check makes a link stop working once its creator is demoted, leaves, or is removed.
    private static void assertUsable(OffsetDateTime revokedAt, boolean expired, boolean exhausted,
                                     boolean creatorCanShare) {
        if (revokedAt != null || !creatorCanShare) {
            throw new InvalidTokenException(INVALID_LINK);
        }
        if (expired) {
            throw new InvalidTokenException("This link has expired");
        }
        if (exhausted) {
            throw new InvalidTokenException("This link has reached its use limit");
        }
    }

    private void consume(GroupShareLinkEntity link) {
        if (groupShareLinkRepository.incrementUse(link.getId()) == 0) {
            throw new InvalidTokenException("This link has reached its use limit");
        }
    }

    private void consume(MeetingShareLinkEntity link) {
        if (meetingShareLinkRepository.incrementUse(link.getId()) == 0) {
            throw new InvalidTokenException("This link has reached its use limit");
        }
    }

    private void rateLimit(String key, int maxAttempts, long windowSeconds) {
        if (attemptCounter.isBlocked(key, maxAttempts)) {
            long retryAfter = attemptCounter.getRemainingWindowSeconds(key);
            throw new RateLimitExceededException(retryAfter > 0 ? retryAfter : windowSeconds);
        }
        attemptCounter.incrementAttempts(key, windowSeconds);
    }

    private ShareLinkResponse toResponse(GroupShareLinkEntity link) {
        boolean usable = !link.isExpired(OffsetDateTime.now()) && !link.isExhausted()
                && groupAuthorization.isAdminOrOwner(link.getCreatedBy().getId(), link.getGroup());
        return new ShareLinkResponse(link.getToken(), url(link.getToken()), link.getJoinMode(),
                link.getExpiresAt(), link.getMaxUses(), link.getUseCount(), link.getCreatedAt(),
                link.getCreatedBy().getUsername(), usable);
    }

    private ShareLinkResponse toResponse(MeetingShareLinkEntity link) {
        MeetingEntity meeting = link.getMeeting();
        boolean usable = !link.isExpired(OffsetDateTime.now()) && !link.isExhausted() && !meeting.hasEnded()
                && meetingAuthorization.canManage(link.getCreatedBy().getId(), meeting);
        return new ShareLinkResponse(link.getToken(), url(link.getToken()), null,
                link.getExpiresAt(), link.getMaxUses(), link.getUseCount(), link.getCreatedAt(),
                link.getCreatedBy().getUsername(), usable);
    }

    private String url(String token) {
        return appBaseUrl + "/join/" + token;
    }
}
