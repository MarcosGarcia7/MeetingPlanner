package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.SearchCursorCodec;
import com.meetingplanner.common.api.SearchQuery;
import com.meetingplanner.common.api.dto.*;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.ForbiddenException;
import com.meetingplanner.common.api.exceptions.GroupNameTakenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.GroupViewerStatus;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupBanEntity;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupBanRepository;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
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
import java.util.*;
import java.util.function.Function;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupAuthorizationService authorization;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBanRepository groupBanRepository;
    private final GroupRepository groupRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final MeetingJoinRequestRepository meetingJoinRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public GroupServiceImpl(GroupAuthorizationService authorization,
                            GroupMemberRepository groupMemberRepository,
                            GroupBanRepository groupBanRepository,
                            GroupRepository groupRepository,
                            GroupInviteRepository groupInviteRepository,
                            GroupJoinRequestRepository groupJoinRequestRepository,
                            MeetingInviteRepository meetingInviteRepository,
                            MeetingJoinRequestRepository meetingJoinRequestRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            BlockService blockService) {
        this.authorization = authorization;
        this.groupMemberRepository = groupMemberRepository;
        this.groupBanRepository = groupBanRepository;
        this.groupRepository = groupRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.meetingJoinRequestRepository = meetingJoinRequestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockService = blockService;
    }

    @Override
    @Transactional
    public GroupResponse create(long userId, GroupCreateRequest request) {
        UserEntity owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String groupName = request.groupName().trim();
        if (groupRepository.existsByGroupName(groupName)) {
            throw new GroupNameTakenException(groupName);
        }

        GroupEntity group = new GroupEntity();
        group.setGroupName(groupName);
        group.setOwner(owner);
        if (request.isPrivate() != null) {
            group.setPrivate(request.isPrivate());
        }
        GroupMemberEntity ownerMember = group.addMember(owner, true);

        GroupEntity saved;
        try {
            saved = groupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            throw new GroupNameTakenException(groupName);
        }
        return toGroupResponse(saved, ownerMember);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse get(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        GroupMemberEntity viewer = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).orElse(null);
        return toGroupResponse(group, viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GroupSummaryResponse> listMine(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupEntity> rows = groupRepository.listMine(
                userId,
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        return toPage(userId, rows, limit, last -> CursorCodec.encodeId(last.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GroupSummaryResponse> search(long viewerId, String query, String cursor, int size) {
        SearchQuery search = SearchQuery.parse(query);
        int limit = CursorCodec.clampSize(size);
        SearchCursorCodec.Cursor decoded = SearchCursorCodec.decode(cursor);
        List<GroupEntity> rows = groupRepository.search(
                search.pattern(),
                decoded == null ? null : decoded.value(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        return toPage(viewerId, rows, limit,
                last -> SearchCursorCodec.encode(last.getGroupName().toLowerCase(Locale.ROOT), last.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listSuggested(long viewerId, int limit) {
        List<Long> ids = groupRepository.findSuggestedIds(viewerId, PageRequest.of(0, CursorCodec.clampSize(limit)));
        return toSummaries(viewerId, findInOrder(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listPopular(long viewerId, int limit) {
        List<Long> ids = groupRepository.findPopularIds(viewerId, PageRequest.of(0, CursorCodec.clampSize(limit)));
        return toSummaries(viewerId, findInOrder(ids));
    }

    private List<GroupEntity> findInOrder(List<Long> ids) {
        Map<Long, GroupEntity> byId = new HashMap<>();
        groupRepository.findAllById(ids).forEach(group -> byId.put(group.getId(), group));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private CursorPage<GroupSummaryResponse> toPage(long viewerId, List<GroupEntity> rows, int size,
                                                    Function<GroupEntity, String> cursorOf) {
        boolean hasMore = rows.size() > size;
        List<GroupEntity> page = hasMore ? rows.subList(0, size) : rows;
        String nextCursor = hasMore && !page.isEmpty() ? cursorOf.apply(page.getLast()) : null;
        return new CursorPage<>(toSummaries(viewerId, page), nextCursor);
    }

    // Resolves counts and the viewer's relationship for the whole page in a fixed number of queries.
    private List<GroupSummaryResponse> toSummaries(long viewerId, List<GroupEntity> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(GroupEntity::getId).toList();
        Set<Long> memberOf = groupMemberRepository.findGroupIdsByUserId(viewerId);
        Set<Long> adminOf = groupMemberRepository.findAdminGroupIdsByUserId(viewerId);
        Map<Long, Long> memberCounts = toCountMap(groupMemberRepository.countByGroupIds(groupIds));
        Map<Long, Long> friendCounts = toCountMap(groupMemberRepository.countFriendMembersByGroupIds(viewerId, groupIds));
        Set<Long> bannedFrom = groupBanRepository.findBannedGroupIds(viewerId, groupIds);
        Map<Long, Long> requestIds = new HashMap<>();
        groupJoinRequestRepository.findByRequesterIdAndStatusAndGroupIdIn(viewerId, RequestStatus.PENDING, groupIds)
                .forEach(request -> requestIds.put(request.getGroup().getId(), request.getId()));
        Map<Long, Long> inviteIds = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        groupInviteRepository.findByRecipientIdAndStatusAndGroupIdIn(viewerId, InviteStatus.PENDING, groupIds)
                .stream()
                .filter(invite -> !invite.isExpired(now))
                .forEach(invite -> inviteIds.put(invite.getGroup().getId(), invite.getId()));

        return groups.stream().map(group -> {
            Long id = group.getId();
            boolean owner = Objects.equals(group.getOwner().getId(), viewerId);
            GroupViewerStatus status;
            if (memberOf.contains(id) || owner) {
                status = GroupViewerStatus.MEMBER;
            } else if (bannedFrom.contains(id)) {
                status = GroupViewerStatus.BANNED;
            } else if (inviteIds.containsKey(id)) {
                status = GroupViewerStatus.INVITED;
            } else if (requestIds.containsKey(id)) {
                status = GroupViewerStatus.REQUESTED;
            } else {
                status = GroupViewerStatus.NONE;
            }
            return new GroupSummaryResponse(
                    id,
                    group.getGroupName(),
                    group.getOwner().getId(),
                    group.isPrivate(),
                    memberCounts.getOrDefault(id, 0L).intValue(),
                    friendCounts.getOrDefault(id, 0L).intValue(),
                    status,
                    status == GroupViewerStatus.REQUESTED ? requestIds.get(id) : null,
                    status == GroupViewerStatus.INVITED ? inviteIds.get(id) : null,
                    GroupAuthorizationService.canCreateMeetings(group, owner, adminOf.contains(id), memberOf.contains(id)));
        }).toList();
    }

    private static Map<Long, Long> toCountMap(List<GroupMemberRepository.GroupCount> counts) {
        Map<Long, Long> map = new HashMap<>();
        counts.forEach(count -> map.put(count.getGroupId(), count.getCount()));
        return map;
    }

    // `viewer` is the requesting user's membership, or null when they aren't a member: non-members get
    // no member list and no viewer settings.
    private GroupResponse toGroupResponse(GroupEntity group, GroupMemberEntity viewer) {
        List<GroupMemberResponse> members = viewer != null
                ? group.getMembers().stream()
                        .map(m -> {
                            UserEntity user = m.getUser();
                            return new GroupMemberResponse(user.getId(), user.getUsername(), user.getFirstName(),
                                    user.getLastName(), user.getAvatarColor(), m.isAdmin());
                        })
                        .toList()
                : List.of();
        return new GroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getOwner().getId(),
                group.isPrivate(),
                (int) groupMemberRepository.countByGroupId(group.getId()),
                group.isMembersCanPost(),
                group.isCommentsEnabled(),
                group.isMembersCanCreate(),
                members,
                viewer != null ? viewer.isShareSchedule() : null);
    }

    @Override
    @Transactional
    public GroupResponse update(long userId, long groupId, GroupUpdateRequest request) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isAdminOrOwner(member, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        String groupName = request.groupName() == null ? null : request.groupName().trim();
        boolean renaming = groupName != null && !Objects.equals(groupName, group.getGroupName());
        boolean changingPrivacy = request.isPrivate() != null && !Objects.equals(request.isPrivate(), group.isPrivate());
        if ((renaming || changingPrivacy) && !authorization.canChangeNameOrPrivacy(member, group)) {
            throw new ForbiddenException("Only the group owner can rename the group or change its privacy");
        }

        if (renaming) {
            if (groupRepository.existsByGroupNameAndIdNot(groupName, groupId)) {
                throw new GroupNameTakenException(groupName);
            }
            group.setGroupName(groupName);
        }

        if (changingPrivacy) {
            group.setPrivate(request.isPrivate());
        }

        if (request.membersCanPost() != null && !Objects.equals(request.membersCanPost(), group.isMembersCanPost())) {
            group.setMembersCanPost(request.membersCanPost());
        }

        if (request.commentsEnabled() != null && !Objects.equals(request.commentsEnabled(), group.isCommentsEnabled())) {
            group.setCommentsEnabled(request.commentsEnabled());
        }

        if (request.membersCanCreate() != null && !Objects.equals(request.membersCanCreate(), group.isMembersCanCreate())) {
            group.setMembersCanCreate(request.membersCanCreate());
        }

        GroupEntity saved;
        try {
            saved = groupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException e) {
            throw new GroupNameTakenException(group.getGroupName());
        }

        return toGroupResponse(saved, member);
    }

    @Override
    @Transactional
    public void delete(long userId, long groupId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isOwner(member, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        groupRepository.delete(group);
    }

    @Override
    @Transactional
    public void join(long userId, long groupId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (authorization.isBanned(userId, group)) {
            throw new ForbiddenException("You are banned from this group");
        }
        if (group.isPrivate()) {
            throw new ForbiddenException("This group is invite-only");
        }

        if (authorization.isMember(userId, group)) {
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
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!authorization.isAdminOrOwner(actorMember, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        UserEntity sender = actorMember.getUser();

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, noInvitesSentMessage(failed));
        }

        return new InviteBatchResponse(invited, failed);
    }

    // The 400 replaces the batch body, so the per-recipient reasons go into its message.
    private static String noInvitesSentMessage(List<InviteFailure> failed) {
        if (failed.size() == 1) {
            InviteFailure only = failed.getFirst();
            return "Could not invite @" + only.username() + ": " + only.reason();
        }
        StringJoiner reasons = new StringJoiner("; ", "No invites could be sent. ", "");
        for (InviteFailure failure : failed) {
            reasons.add("@" + failure.username() + ": " + failure.reason());
        }
        return reasons.toString();
    }

    private InviteResponse inviteOne(GroupEntity group, UserEntity sender, MemberUsernameRequest request) {
        UserEntity recipient = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (recipient.getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }
        blockService.assertNotBlocked(sender.getId(), recipient.getId(), "You cannot invite this user");
        if (authorization.isBanned(recipient.getId(), group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is banned from this group");
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
        notificationService.queueEmailToForUser(recipient, recipientEmail, "GROUP_INVITE", Map.of(
                "name", recipient.getUsername(),
                "sender", sender.getUsername(),
                "link", link), "group_invites");

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
                invite.getGroup() == null ? null : invite.getGroup().getGroupName(),
                null,
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

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isAdminOrOwner(member, group)) {
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
        if (actorUserId == targetUserId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove yourself, leave group instead");
        }

        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isAdminOrOwner(actorMember, group) || authorization.isOwner(member, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        if (authorization.isOwner(actorMember, group)) {
            groupMemberRepository.delete(member);
            return;
        }

        if (member.isAdmin()) {
            throw new ResourceNotFoundException("Group not found");
        }

        groupMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void banMember(long actorUserId, long groupId, long targetUserId) {
        if (actorUserId == targetUserId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot ban yourself");
        }

        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isAdminOrOwner(actorMember, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (authorization.isOwner(member, group)) {
            throw new ForbiddenException("You cannot ban the group owner");
        }

        boolean actorIsOwner = authorization.isOwner(actorMember, group);
        if (member.isAdmin() && !actorIsOwner) {
            throw new ForbiddenException("Only the group owner can ban an admin");
        }
        if (groupBanRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already banned");
        }

        groupMemberRepository.delete(member);

        GroupBanEntity ban = new GroupBanEntity();
        ban.setGroup(group);
        ban.setUser(member.getUser());
        ban.setBannedBy(actorMember.getUser());
        ban.setBannedByOwner(actorIsOwner);
        ban.setBannedAt(OffsetDateTime.now());
        groupBanRepository.save(ban);

        invalidatePendingInGroup(groupId, targetUserId, ban.getBannedAt());
    }

    private void invalidatePendingInGroup(long groupId, long userId, OffsetDateTime now) {
        groupInviteRepository.declinePendingForRecipientInGroup(groupId, userId, now);
        meetingInviteRepository.declinePendingForRecipientInGroupMeetings(groupId, userId, now);
        groupJoinRequestRepository.cancelPendingForRequesterInGroup(groupId, userId, now);
        meetingJoinRequestRepository.cancelPendingForRequesterInGroupMeetings(groupId, userId, now);
    }

    @Override
    @Transactional
    public void unbanMember(long actorUserId, long groupId, long targetUserId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isAdminOrOwner(actorMember, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        GroupBanEntity ban = groupBanRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ban not found"));

        if (ban.isBannedByOwner() && !authorization.isOwner(actorMember, group)) {
            throw new ForbiddenException("Only the group owner can unban this user");
        }

        groupBanRepository.delete(ban);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GroupBanResponse> listBans(long actorUserId, long groupId, String cursor, int size) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!authorization.isAdminOrOwner(actorUserId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupBanEntity> rows = groupBanRepository.listByGroup(
                groupId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupBanEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<GroupBanResponse> items = page.stream().map(GroupServiceImpl::toBanResponse).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getBannedAt(), page.getLast().getUser().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private static GroupBanResponse toBanResponse(GroupBanEntity ban) {
        return new GroupBanResponse(
                ban.getUser().getId(),
                ban.getUser().getUsername(),
                ban.getBannedBy().getId(),
                ban.getBannedBy().getUsername(),
                ban.isBannedByOwner(),
                ban.getBannedAt());
    }

    @Override
    @Transactional
    public void promoteOrDemoteMember(long actorUserId, long groupId, long targetUserId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        if (!authorization.isOwner(actorMember, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        member.setAdmin(!member.isAdmin());

        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void setShareSchedule(long userId, long groupId, boolean shareSchedule) {
        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));
        member.setShareSchedule(shareSchedule);
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void transferOwnership(long actorUserId, long groupId, long targetUserId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity actorMember = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!authorization.isOwner(actorMember, group)) {
            throw new ResourceNotFoundException("Group not found");
        }

        if (actorUserId == targetUserId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already own this group");
        }

        GroupMemberEntity targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found"));

        group.setOwner(targetMember.getUser());
        targetMember.setAdmin(true);
        groupRepository.save(group);
        groupMemberRepository.save(targetMember);
    }

    @Override
    @Transactional
    public void leave(long userId, long groupId) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (authorization.isOwner(member, group)) {
            if (groupMemberRepository.countByGroupId(groupId) > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Transfer ownership or delete the group to leave");
            }
            groupRepository.delete(group);
            return;
        }

        groupMemberRepository.delete(member);
    }

    // Called after the membership check, so an ACCEPTED invite belongs to someone who has since left or
    // been removed and can be reset like a declined one.
    private GroupInviteEntity reuseForReinvite(GroupInviteEntity existing) {
        if (existing.getStatus() == InviteStatus.PENDING && !existing.isExpired(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invite already exists for this email");
        }
        return existing;
    }
}