package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.GroupUpdateRequest;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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
        if (!authorization.isMember(userId, groupId) && !authorization.isOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> listMine(long userId) {
        return List.of();
    }

    @Override
    @Transactional
    public GroupResponse update(long userId, long groupId, GroupUpdateRequest request) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void delete(long userId, long groupId) {
        if (!authorization.isOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void invite(long userId, long groupId, MemberUsernameRequest request) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }

        UserEntity recipient = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (recipient.getEmailVerifiedAt() == null) {
            throw new EmailNotVerifiedException();
        }

        GroupInviteEntity invite = groupInviteRepository
                .findByGroupIdAndRecipientEmail(groupId, recipient.getEmail())
                .map(this::reuseForReinvite)
                .orElseGet(GroupInviteEntity::new);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        UserEntity sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String rawToken = InviteTokens.generateRawToken();
        invite.setSender(sender);
        invite.setGroup(group);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipient.getEmail());
        invite.setTokenHash(InviteTokens.hash(rawToken));
        invite.setStatus(InviteStatus.PENDING);
        invite.setRespondedAt(null);
        groupInviteRepository.save(invite);

        String link = appBaseUrl + "/invites/group?token=" + rawToken;
        notificationService.queueEmailTo(recipient.getEmail(), "GROUP_INVITE", Map.of(
                "name", recipient.getUsername(),
                "sender", sender.getUsername(),
                "group", group.getGroupName(),
                "link", link));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InviteResponse> listInvites(long userId, long groupId) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        return List.of();
    }

    @Override
    @Transactional
    public void removeMember(long actorUserId, long groupId, long targetUserId) {
        if (!authorization.isAdminOrOwner(actorUserId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void promoteMember(long actorUserId, long groupId, long targetUserId) {
        if (!authorization.isAdminOrOwner(actorUserId, groupId)) {
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
        if (!authorization.isMember(userId, groupId) && !authorization.isOwner(userId, groupId)) {
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