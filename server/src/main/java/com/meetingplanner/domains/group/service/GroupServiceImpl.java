package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.*;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.ForbiddenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupAuthorizationService authorization;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public GroupServiceImpl(GroupAuthorizationService authorization,
                            GroupMemberRepository groupMemberRepository,
                            GroupRepository groupRepository,
                            GroupInviteRepository groupInviteRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.authorization = authorization;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public GroupResponse create(long userId, GroupCreateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse get(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        boolean includeMembers = authorization.isOwner(userId, group) || authorization.isMember(userId, group);
        return toGroupResponse(group, includeMembers);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GroupResponse> listMine(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupEntity> rows = groupRepository.listMine(
                userId,
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<GroupResponse> items = page.stream().map(group -> toGroupResponse(group, true)).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encodeId(page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private GroupResponse toGroupResponse(GroupEntity group, boolean includeMembers) {
        List<GroupMemberResponse> members = includeMembers
                ? group.getMembers().stream()
                        .map(m -> new GroupMemberResponse(m.getUser().getId(), m.getUser().getUsername(), m.isAdmin()))
                        .toList()
                : List.of();
        return new GroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getOwner().getId(),
                group.isPrivate(),
                (int) groupMemberRepository.countByGroupId(group.getId()),
                members);
    }

    @Override
    @Transactional
    public GroupResponse update(long userId, long groupId, GroupUpdateRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isAdminOrOwner(userId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void delete(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isOwner(userId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void join(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (group.isPrivate()) {
            throw new ForbiddenException("This group is invite-only");
        }
        if (authorization.isOwner(userId, group)
                || authorization.isMember(userId, group)) {
            return;
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(group);
        member.setUser(user);
        member.setAdmin(false);
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public InviteBatchResponse inviteBatch(long actorUserId, long groupId, GroupInviteBatchRequest request) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isAdminOrOwner(actorUserId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        UserEntity sender = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<InviteResponse> invited = new ArrayList<>();
        List<InviteFailure> failed = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (MemberUsernameRequest recipient : request.recipients()) {
            String dedupeKey = recipient.username().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(dedupeKey)) {
                failed.add(failure(recipient, "Duplicate recipient in request"));
                continue;
            }
            try {
                invited.add(inviteOne(group, sender, recipient));
            } catch (ResourceNotFoundException | EmailNotVerifiedException | ResponseStatusException e) {
                failed.add(failure(recipient, reasonFor(e)));
            }
        }

        if (invited.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invites could be sent");
        }

        return new InviteBatchResponse(invited, failed);
    }

    private InviteResponse inviteOne(GroupEntity group, UserEntity sender, MemberUsernameRequest request) {
        UserEntity recipient = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (recipient.getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
        }
        if (recipient.getEmailVerifiedAt() == null) {
            throw new EmailNotVerifiedException();
        }
        String recipientEmail = recipient.getEmail();

        GroupInviteEntity invite = groupInviteRepository
                .findByGroupIdAndRecipientEmail(group.getId(), recipientEmail)
                .map(this::reuseForReinvite)
                .orElseGet(GroupInviteEntity::new);

        String rawToken = InviteTokens.generateRawToken();
        invite.setSender(sender);
        invite.setGroup(group);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipientEmail);
        invite.setTokenHash(InviteTokens.hash(rawToken));
        invite.setStatus(InviteStatus.PENDING);
        invite.setRespondedAt(null);
        invite.setTimeSent(OffsetDateTime.now());
        groupInviteRepository.save(invite);

        String link = appBaseUrl + "/invites/group?token=" + rawToken;
        notificationService.queueEmailTo(recipientEmail, "GROUP_INVITE", Map.of(
                "name", recipient.getUsername(),
                "sender", sender.getUsername(),
                "link", link));

        return toInviteResponse(invite);
    }

    private static InviteResponse toInviteResponse(GroupInviteEntity invite) {
        UserEntity sender = invite.getSender();
        return new InviteResponse(
                invite.getId(),
                "GROUP",
                invite.getStatus(),
                sender.getId(),
                sender.getUsername(),
                invite.getRecipient() == null ? null : invite.getRecipient().getId(),
                invite.getRecipientEmail(),
                invite.getGroup() == null ? null : invite.getGroup().getId(),
                null,
                List.of());
    }

    private static InviteFailure failure(MemberUsernameRequest recipient, String reason) {
        return new InviteFailure(recipient.username(), null, reason);
    }

    private static String reasonFor(RuntimeException e) {
        if (e instanceof EmailNotVerifiedException || e instanceof ResourceNotFoundException) {
            return e.getMessage();
        }
        if (e instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getReason() != null
                    ? responseStatusException.getReason()
                    : HttpStatus.valueOf(responseStatusException.getStatusCode().value()).getReasonPhrase();
        }
        return "Invite failed";
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<InviteResponse> listInvites(long userId, long groupId, String cursor, int size) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isAdminOrOwner(userId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupInviteEntity> rows = groupInviteRepository.listByGroup(
                groupId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupInviteEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<InviteResponse> items = page.stream().map(GroupServiceImpl::toInviteResponse).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getTimeSent(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional
    public void removeMember(long actorUserId, long groupId, long targetUserId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isAdminOrOwner(actorUserId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void promoteMember(long actorUserId, long groupId, long targetUserId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isAdminOrOwner(actorUserId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void setShareSchedule(long userId, long groupId, boolean shareSchedule) {
        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        member.setShareSchedule(shareSchedule);
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void leave(long userId, long groupId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!authorization.isMember(userId, group) && !authorization.isOwner(userId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private GroupInviteEntity reuseForReinvite(GroupInviteEntity existing) {
        if (existing.getStatus() == InviteStatus.PENDING || existing.getStatus() == InviteStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invite already exists for this email");
        }
        return existing;
    }
}