package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InviteServiceImpl implements InviteService {

    private final GroupInviteRepository groupInviteRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public InviteServiceImpl(GroupInviteRepository groupInviteRepository,
                             GroupMemberRepository groupMemberRepository,
                             MeetingInviteRepository meetingInviteRepository,
                             MeetingRepository meetingRepository,
                             MeetingParticipantRepository meetingParticipantRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.groupInviteRepository = groupInviteRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InviteResponse> listMine(long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<Long, InviteResponse> byId = new LinkedHashMap<>();
        for (GroupInviteEntity invite : groupInviteRepository.findByRecipientId(userId)) {
            byId.put(invite.getId(), toGroupResponse(invite));
        }
        for (GroupInviteEntity invite : groupInviteRepository.findByRecipientEmail(user.getEmail())) {
            byId.put(invite.getId(), toGroupResponse(invite));
        }
        for (MeetingInviteEntity invite : meetingInviteRepository.findByRecipientId(userId)) {
            byId.put(invite.getId(), toMeetingResponse(invite));
        }
        for (MeetingInviteEntity invite : meetingInviteRepository.findByRecipientEmail(user.getEmail())) {
            byId.put(invite.getId(), toMeetingResponse(invite));
        }
        return new ArrayList<>(byId.values());
    }

    @Override
    @Transactional
    public InviteResponse acceptGroupInvite(long userId, long inviteId) {
        GroupInviteEntity invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getRecipient() == null || !invite.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Invite not found");
        }
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
        respondPendingOnly(invite, InviteStatus.ACCEPTED, "Invite has already been responded to");
        if (invite.getRecipient() != null) {
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
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toMeetingResponse(invite);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResponse viewGroupInvite(String token) {
        return toGroupResponse(groupByToken(token));
    }

    @Override
    @Transactional(readOnly = true)
    public InviteResponse viewMeetingInvite(String token) {
        return toMeetingResponse(meetingByToken(token));
    }

    @Override
    @Transactional
    public InviteResponse acceptGroupInviteByToken(String token) {
        GroupInviteEntity invite = groupByToken(token);
        acceptGroup(invite);
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse declineGroupInviteByToken(String token) {
        GroupInviteEntity invite = groupByToken(token);
        respondPendingOnly(invite, InviteStatus.DECLINED, "Invite has already been responded to");
        return toGroupResponse(invite);
    }

    @Override
    @Transactional
    public InviteResponse acceptMeetingInviteByToken(String token) {
        MeetingInviteEntity invite = meetingByToken(token);
        respondPendingOnly(invite, InviteStatus.ACCEPTED, "Invite has already been responded to");
        if (invite.getRecipient() != null) {
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
        MeetingEntity meeting = invite.getMeeting();
        if (invite.getRecipient() != null) {
            meetingParticipantRepository.deleteByMeetingIdAndUserId(meeting.getId(), invite.getRecipient().getId());
        } else {
            removeExternalParticipant(meeting, invite.getRecipientEmail());
        }
        invite.setStatus(InviteStatus.LEFT);
        invite.setRespondedAt(OffsetDateTime.now());
        meetingInviteRepository.save(invite);
        return toMeetingResponse(invite);
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
        Long recipientId = invite.getRecipient().getId();
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, recipientId)) {
            GroupMemberEntity member = new GroupMemberEntity();
            member.setGroup(invite.getGroup());
            member.setUser(invite.getRecipient());
            groupMemberRepository.save(member);
        }
        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setRespondedAt(OffsetDateTime.now());
        groupInviteRepository.save(invite);
    }

    private void addParticipant(MeetingEntity meeting, UserEntity user) {
        if (!meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            MeetingParticipantEntity participant = new MeetingParticipantEntity();
            participant.setMeeting(meeting);
            participant.setUser(user);
            meetingParticipantRepository.save(participant);
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
        invite.setStatus(status);
        invite.setRespondedAt(OffsetDateTime.now());
        meetingInviteRepository.save(invite);
    }

    private void respondPendingOnly(GroupInviteEntity invite, InviteStatus status, String errorMessage) {
        requirePending(invite, errorMessage);
        invite.setStatus(status);
        invite.setRespondedAt(OffsetDateTime.now());
        groupInviteRepository.save(invite);
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
    }

    private void sendAcceptedEmail(MeetingInviteEntity invite) {
        String rawToken = InviteTokens.generateRawToken();
        invite.setTokenHash(InviteTokens.hash(rawToken));
        meetingInviteRepository.save(invite);

        MeetingEntity meeting = invite.getMeeting();
        String title = meeting.getDescription() != null && !meeting.getDescription().isBlank()
                ? meeting.getDescription()
                : "your meeting";
        String recipientName = invite.getRecipient() != null
                ? invite.getRecipient().getUsername()
                : emailDisplayName(invite.getRecipientEmail());
        String leaveLink = appBaseUrl + "/invites/meeting?token=" + rawToken;
        notificationService.queueEmailTo(invite.getRecipientEmail(), "MEETING_INVITE_ACCEPTED", Map.of(
                "name", recipientName,
                "title", title,
                "link", leaveLink));
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
                null);
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
                invite.getMeeting() == null ? null : invite.getMeeting().getId());
    }
}