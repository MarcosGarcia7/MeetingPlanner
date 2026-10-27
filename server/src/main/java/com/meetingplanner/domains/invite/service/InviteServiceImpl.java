package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
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
public class InviteServiceImpl implements InviteService {

    private final GroupInviteRepository groupInviteRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MeetingConflictService conflictService;
    private final BlockService blockService;
    private final GroupAuthorizationService groupAuthorization;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public InviteServiceImpl(GroupInviteRepository groupInviteRepository,
                             GroupMemberRepository groupMemberRepository,
                             GroupRepository groupRepository,
                             MeetingInviteRepository meetingInviteRepository,
                             MeetingRepository meetingRepository,
                             MeetingParticipantRepository meetingParticipantRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             MeetingConflictService conflictService,
                             BlockService blockService,
                             GroupAuthorizationService groupAuthorization) {
        this.groupInviteRepository = groupInviteRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.conflictService = conflictService;
        this.blockService = blockService;
        this.groupAuthorization = groupAuthorization;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<InviteResponse> listMine(long userId, InviteStatus status, TargetKind kind,
                                               String cursor, int size) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        String cursorKind = decoded == null ? null : decoded.kind();
        Long cursorId = decoded == null ? null : decoded.id();

        // A kind filter skips the other source entirely; the merged keyset order still holds for one source.
        List<GroupInviteEntity> groupInvites = kind == null || kind == TargetKind.GROUP
                ? groupInviteRepository.listForRecipient(
                        userId, user.getEmail(), status, OffsetDateTime.now().minus(GroupInviteEntity.EXPIRY),
                        "GROUP", cursorTime, cursorKind, cursorId,
                        PageRequest.of(0, limit + 1))
                : List.of();
        List<MeetingInviteEntity> meetingInvites = kind == null || kind == TargetKind.MEETING
                ? meetingInviteRepository.listForRecipient(
                        userId, user.getEmail(), status, "MEETING", cursorTime, cursorKind, cursorId,
                        PageRequest.of(0, limit + 1))
                : List.of();

        List<FeedEntry> entries = new ArrayList<>();
        for (GroupInviteEntity invite : groupInvites) {
            entries.add(new FeedEntry(toGroupResponse(invite), invite.getTimeSent(), "GROUP", invite.getId()));
        }
        for (MeetingInviteEntity invite : meetingInvites) {
            entries.add(new FeedEntry(toMeetingResponse(invite), invite.getTimeSent(), "MEETING", invite.getId()));
        }
        entries.sort(Comparator
                .comparing(FeedEntry::timeSent).reversed()
                .thenComparing(FeedEntry::kind, Comparator.reverseOrder())
                .thenComparing(FeedEntry::id, Comparator.reverseOrder()));

        boolean hasMore = entries.size() > limit;
        List<FeedEntry> page = hasMore ? entries.subList(0, limit) : entries;
        List<InviteResponse> items = page.stream().map(FeedEntry::response).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().timeSent(), page.getLast().kind(), page.getLast().id())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private record FeedEntry(InviteResponse response, OffsetDateTime timeSent, String kind, long id) {
    }

    @Override
    @Transactional
    public InviteResponse acceptGroupInvite(long userId, long inviteId) {
        GroupInviteEntity invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getRecipient() == null || !invite.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Invite not found");
        }
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        acceptGroup(invite);
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse declineGroupInvite(long userId, long inviteId) {
        GroupInviteEntity invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getRecipient() == null || !invite.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Invite not found");
        }
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse acceptMeetingInvite(long userId, long inviteId) {
        MeetingInviteEntity invite = meetingInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getRecipient() == null || !invite.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Invite not found");
        }
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        assertNotBannedFromMeetingGroup(invite.getMeeting(), invite.getRecipient());
        lockMeeting(invite.getMeeting());
        respondPendingOnly(invite, InviteStatus.ACCEPTED, "Invite has already been responded to");
        if (invite.getRecipient() != null) {
            assertNoScheduleConflict(invite.getRecipient(), invite.getMeeting());
            addParticipant(invite.getMeeting(), invite.getRecipient());
        }
        sendAcceptedEmail(invite);
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse declineMeetingInvite(long userId, long inviteId) {
        MeetingInviteEntity invite = meetingInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getRecipient() == null || !invite.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Invite not found");
        }
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResponse viewGroupInvite(String token) {
        GroupInviteEntity invite = groupByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        return toGroupResponse(invite);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResponse viewMeetingInvite(String token) {
        MeetingInviteEntity invite = meetingByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse acceptGroupInviteByToken(String token) {
        GroupInviteEntity invite = groupByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        acceptGroup(invite);
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse declineGroupInviteByToken(String token) {
        GroupInviteEntity invite = groupByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse acceptMeetingInviteByToken(String token) {
        MeetingInviteEntity invite = meetingByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        assertNotBannedFromMeetingGroup(invite.getMeeting(), invite.getRecipient());
        lockMeeting(invite.getMeeting());
        respondPendingOnly(invite, InviteStatus.ACCEPTED, "Invite has already been responded to");
        if (invite.getRecipient() != null) {
            assertNoScheduleConflict(invite.getRecipient(), invite.getMeeting());
            addParticipant(invite.getMeeting(), invite.getRecipient());
        } else {
            addExternalParticipant(invite.getMeeting(), invite.getRecipientEmail());
        }
        sendAcceptedEmail(invite);
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse declineMeetingInviteByToken(String token) {
        MeetingInviteEntity invite = meetingByToken(token);
        assertNotBlockedForPending(invite.getSender(), invite.getRecipient(), invite.getStatus());
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse leaveMeetingInviteByToken(String token) {
        MeetingInviteEntity invite = meetingByToken(token);
        if (invite.getStatus() != InviteStatus.ACCEPTED) {
            throw new InvalidTokenException("Invite has not been accepted");
        }
        MeetingEntity meeting = lockMeeting(invite.getMeeting());
        transition(invite, InviteStatus.ACCEPTED, InviteStatus.LEFT, "Invite has not been accepted");
        if (invite.getRecipient() != null) {
            meetingParticipantRepository.deleteByMeetingIdAndUserId(meeting.getId(), invite.getRecipient().getId());
        } else {
            removeExternalParticipant(meeting, invite.getRecipientEmail());
        }
        return toMeetingResponse(invite);
    }

    private void assertNotBlockedForPending(UserEntity sender, UserEntity recipient, InviteStatus status) {
        if (status == InviteStatus.PENDING
                && sender != null
                && recipient != null
                && blockService.isBlocked(sender.getId(), recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite is no longer available");
        }
    }

    private GroupInviteEntity groupByToken(String token) {
        return groupInviteRepository.findByTokenHash(InviteTokens.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid invite link"));
    }

    private MeetingInviteEntity meetingByToken(String token) {
        return meetingInviteRepository.findByTokenHash(InviteTokens.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid invite link"));
    }

    private void acceptGroup(GroupInviteEntity invite) {
        requirePending(invite, "Invite has already been responded to");
        Long groupId = invite.getGroup().getId();
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        Long recipientId = invite.getRecipient().getId();
        if (groupAuthorization.isBanned(recipientId, group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this group");
        }
        transition(invite, InviteStatus.PENDING, InviteStatus.ACCEPTED, "Invite has already been responded to");
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, recipientId)) {
            GroupMemberEntity member = new GroupMemberEntity();
            member.setGroup(group);
            member.setUser(invite.getRecipient());
            groupMemberRepository.save(member);
        }
    }

    private void assertNotBannedFromMeetingGroup(MeetingEntity meeting, UserEntity recipient) {
        if (recipient == null || meeting.getGroup() == null) {
            return;
        }
        GroupEntity group = groupRepository.findByIdForUpdate(meeting.getGroup().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (groupAuthorization.isBanned(recipient.getId(), group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this group");
        }
    }

    // Taken after any group lock (group -> meeting -> invite). Serializes participant changes with meeting
    // delete/removeParticipant and keeps externalParticipantNames read-modify-write from losing updates.
    private MeetingEntity lockMeeting(MeetingEntity meeting) {
        return meetingRepository.findByIdForUpdate(meeting.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
    }

    private void assertNoScheduleConflict(UserEntity recipient, MeetingEntity meeting) {
        boolean conflicts = !conflictService.findScheduleConflicts(
                recipient.getId(), meeting.getStartTime(), meeting.getEndTime(), meeting.getId()).isEmpty();
        if (conflicts) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This meeting conflicts with your schedule");
        }
    }

    private void addParticipant(MeetingEntity meeting, UserEntity user) {
        if (!meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            MeetingParticipantEntity participant = new MeetingParticipantEntity();
            participant.setMeeting(meeting);
            participant.setUser(user);
            try {
                meetingParticipantRepository.saveAndFlush(participant);
            } catch (DataIntegrityViolationException e) {
                // Safety net only; the meeting lock already serializes participant inserts.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a participant of this meeting");
            }
        }
    }

    private void addExternalParticipant(MeetingEntity meeting, String email) {
        List<String> names = meeting.getExternalParticipantNames();
        if (names == null) {
            names = new ArrayList<>();
            meeting.setExternalParticipantNames(names);
        }
        if (!names.contains(email)) {
            names.add(email);
            meetingRepository.save(meeting);
        }
    }

    private void removeExternalParticipant(MeetingEntity meeting, String email) {
        List<String> names = meeting.getExternalParticipantNames();
        if (names != null && names.remove(email)) {
            meetingRepository.save(meeting);
        }
    }

    private void respondPendingOnly(MeetingInviteEntity invite, InviteStatus status, String errorMessage) {
        requirePending(invite, errorMessage);
        transition(invite, InviteStatus.PENDING, status, errorMessage);
    }

    private void respondPendingOnly(GroupInviteEntity invite, InviteStatus status, String errorMessage) {
        requirePending(invite, errorMessage);
        transition(invite, InviteStatus.PENDING, status, errorMessage);
    }

    // Conditional UPDATE ... WHERE status = :from so concurrent accept/decline/leave calls can't both win.
    private void transition(MeetingInviteEntity invite, InviteStatus from, InviteStatus to, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        if (meetingInviteRepository.transitionStatus(invite.getId(), from, to, now) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
        invite.setStatus(to);
        invite.setRespondedAt(now);
    }

    private void transition(GroupInviteEntity invite, InviteStatus from, InviteStatus to, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        if (groupInviteRepository.transitionStatus(invite.getId(), from, to, now) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
        invite.setStatus(to);
        invite.setRespondedAt(now);
    }

    private void requirePending(MeetingInviteEntity invite, String errorMessage) {
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
    }

    private void requirePending(GroupInviteEntity invite, String errorMessage) {
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
        if (invite.isExpired(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invite has expired");
        }
    }

    private void sendAcceptedEmail(MeetingInviteEntity invite) {
        String rawToken = InviteTokens.generateRawToken();
        invite.setTokenHash(InviteTokens.hash(rawToken));
        meetingInviteRepository.save(invite);

        MeetingEntity meeting = invite.getMeeting();
        String recipientName = invite.getRecipient() != null
                ? invite.getRecipient().getUsername()
                : emailDisplayName(invite.getRecipientEmail());
        String leaveLink = appBaseUrl + "/invites/meeting?token=" + rawToken;
        notificationService.queueEmailToForUser(invite.getRecipient(), invite.getRecipientEmail(), "MEETING_INVITE_ACCEPTED", Map.of(
                "name", recipientName,
                "title", meeting.getTitle(),
                "link", leaveLink), "meeting_invites");
    }

    private static String emailDisplayName(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private static InviteResponse toGroupResponse(GroupInviteEntity invite) {
        GroupEntity group = invite.getGroup();
        UserEntity sender = invite.getSender();
        return new InviteResponse(
                invite.getId(),
                "GROUP",
                invite.getStatus(),
                sender.getId(),
                sender.getUsername(),
                invite.getRecipient() == null ? null : invite.getRecipient().getId(),
                invite.getRecipientEmail(),
                group == null ? null : group.getId(),
                group == null ? null : group.getGroupName(),
                null,
                null,
                List.of());
    }

    private static InviteResponse toMeetingResponse(MeetingInviteEntity invite) {
        UserEntity sender = invite.getSender();
        return new InviteResponse(
                invite.getId(),
                "MEETING",
                invite.getStatus(),
                sender.getId(),
                sender.getUsername(),
                invite.getRecipient() == null ? null : invite.getRecipient().getId(),
                invite.getRecipientEmail(),
                null,
                null,
                invite.getMeeting() == null ? null : invite.getMeeting().getId(),
                invite.getMeeting() == null ? null : invite.getMeeting().getTitle(),
                List.of());
    }
}