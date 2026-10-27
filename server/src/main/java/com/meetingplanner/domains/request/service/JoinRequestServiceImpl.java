package com.meetingplanner.domains.request.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupJoinRequestEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.service.MeetingAuthorizationService;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JoinRequestServiceImpl implements JoinRequestService {

    private static final String KIND_GROUP = "GROUP";
    private static final String KIND_MEETING = "MEETING";
    private static final String CATEGORY_GROUP = "group_invites";
    private static final String CATEGORY_MEETING = "meeting_invites";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingJoinRequestRepository meetingJoinRequestRepository;
    private final UserRepository userRepository;
    private final GroupAuthorizationService groupAuthorization;
    private final MeetingAuthorizationService meetingAuthorization;
    private final MeetingConflictService conflictService;
    private final NotificationService notificationService;
    private final BlockService blockService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public JoinRequestServiceImpl(GroupRepository groupRepository,
                                  GroupMemberRepository groupMemberRepository,
                                  GroupJoinRequestRepository groupJoinRequestRepository,
                                  MeetingRepository meetingRepository,
                                  MeetingParticipantRepository meetingParticipantRepository,
                                  MeetingJoinRequestRepository meetingJoinRequestRepository,
                                  UserRepository userRepository,
                                  GroupAuthorizationService groupAuthorization,
                                  MeetingAuthorizationService meetingAuthorization,
                                  MeetingConflictService conflictService,
                                  NotificationService notificationService,
                                  BlockService blockService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingJoinRequestRepository = meetingJoinRequestRepository;
        this.userRepository = userRepository;
        this.groupAuthorization = groupAuthorization;
        this.meetingAuthorization = meetingAuthorization;
        this.conflictService = conflictService;
        this.notificationService = notificationService;
        this.blockService = blockService;
    }

    @Override
    @Transactional
    public JoinRequestResponse requestGroupJoin(long userId, long groupId) {
        // Locked so the request serializes with ban/approve on this group.
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (groupAuthorization.isBanned(userId, group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this group");
        }
        if (!group.isPrivate()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This group is open to join");
        }
        if (groupAuthorization.isOwner(userId, group) || groupAuthorization.isMember(userId, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group");
        }
        if (groupJoinRequestRepository.existsByGroupIdAndRequesterIdAndStatus(groupId, userId, RequestStatus.PENDING)) {
            throw alreadyPending();
        }

        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GroupJoinRequestEntity request = new GroupJoinRequestEntity();
        request.setRequester(requester);
        request.setGroup(group);
        request.setStatus(RequestStatus.PENDING);
        request.setTimeSent(OffsetDateTime.now());
        try {
            groupJoinRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            throw alreadyPending();
        }

        notifyGroupApprovers(group, requester);

        return toGroupResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse requestMeetingJoin(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (meeting.getGroup() == null) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        // Lock order: group -> meeting.
        GroupEntity group = groupRepository.findByIdForUpdate(meeting.getGroup().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        meeting = lockMeeting(meetingId);
        if (meeting.isPrivate() || group.isPrivate()) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        if (groupAuthorization.isBanned(userId, group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot request to join this meeting");
        }
        if (meetingAuthorization.isParticipantOrCreator(userId, meeting)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a participant of this meeting");
        }
        if (blockService.isBlocked(userId, meeting.getCreator().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot request to join this meeting");
        }
        if (meetingJoinRequestRepository.existsByMeetingIdAndRequesterIdAndStatus(
                meetingId, userId, RequestStatus.PENDING)) {
            throw alreadyPending();
        }

        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MeetingJoinRequestEntity request = new MeetingJoinRequestEntity();
        request.setRequester(requester);
        request.setMeeting(meeting);
        request.setStatus(RequestStatus.PENDING);
        request.setTimeSent(OffsetDateTime.now());
        try {
            meetingJoinRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            throw alreadyPending();
        }

        notifyMeetingApprovers(meeting, requester);

        return toMeetingResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<JoinRequestResponse> listMine(long userId, RequestStatus status, TargetKind kind,
                                                    String cursor, int size) {
        return mergedFeed(kind, cursor, size,
                (cursorTime, cursorKind, cursorId, page) -> groupJoinRequestRepository.listForRequester(
                        userId, status, KIND_GROUP, cursorTime, cursorKind, cursorId, page),
                (cursorTime, cursorKind, cursorId, page) -> meetingJoinRequestRepository.listForRequester(
                        userId, status, KIND_MEETING, cursorTime, cursorKind, cursorId, page));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<JoinRequestResponse> listIncoming(long userId, RequestStatus status, TargetKind kind,
                                                        String cursor, int size) {
        return mergedFeed(kind, cursor, size,
                (cursorTime, cursorKind, cursorId, page) -> groupJoinRequestRepository.listForManager(
                        userId, status, KIND_GROUP, cursorTime, cursorKind, cursorId, page),
                (cursorTime, cursorKind, cursorId, page) -> meetingJoinRequestRepository.listForManager(
                        userId, status, KIND_MEETING, cursorTime, cursorKind, cursorId, page));
    }

    @FunctionalInterface
    private interface FeedSource<T> {
        List<T> fetch(OffsetDateTime cursorTime, String cursorKind, Long cursorId, Pageable page);
    }

    // Merges group and meeting requests into one keyset feed ordered by (timeSent, kind, id) descending.
    private CursorPage<JoinRequestResponse> mergedFeed(TargetKind kind, String cursor, int size,
                                                       FeedSource<GroupJoinRequestEntity> groups,
                                                       FeedSource<MeetingJoinRequestEntity> meetings) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        String cursorKind = decoded == null ? null : decoded.kind();
        Long cursorId = decoded == null ? null : decoded.id();

        // A kind filter skips the other source entirely; the merged keyset order still holds for one source.
        List<GroupJoinRequestEntity> groupRows = kind == null || kind == TargetKind.GROUP
                ? groups.fetch(cursorTime, cursorKind, cursorId, PageRequest.of(0, limit + 1))
                : List.of();
        List<MeetingJoinRequestEntity> meetingRows = kind == null || kind == TargetKind.MEETING
                ? meetings.fetch(cursorTime, cursorKind, cursorId, PageRequest.of(0, limit + 1))
                : List.of();

        List<FeedEntry> entries = new ArrayList<>();
        for (GroupJoinRequestEntity request : groupRows) {
            entries.add(new FeedEntry(toGroupResponse(request), request.getTimeSent(), KIND_GROUP, request.getId()));
        }
        for (MeetingJoinRequestEntity request : meetingRows) {
            entries.add(new FeedEntry(toMeetingResponse(request), request.getTimeSent(), KIND_MEETING, request.getId()));
        }
        entries.sort(Comparator
                .comparing(FeedEntry::timeSent).reversed()
                .thenComparing(FeedEntry::kind, Comparator.reverseOrder())
                .thenComparing(FeedEntry::id, Comparator.reverseOrder()));

        boolean hasMore = entries.size() > limit;
        List<FeedEntry> page = hasMore ? entries.subList(0, limit) : entries;
        List<JoinRequestResponse> items = page.stream().map(FeedEntry::response).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().timeSent(), page.getLast().kind(), page.getLast().id())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private record FeedEntry(JoinRequestResponse response, OffsetDateTime timeSent, String kind, long id) {
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<JoinRequestResponse> listForGroup(long actorId, long groupId, RequestStatus status, String cursor,
                                                         int size) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!groupAuthorization.isAdminOrOwner(actorId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupJoinRequestEntity> rows = groupJoinRequestRepository.listForGroup(
                groupId,
                status,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupJoinRequestEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<JoinRequestResponse> items = page.stream().map(JoinRequestServiceImpl::toGroupResponse).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getTimeSent(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<JoinRequestResponse> listForMeeting(long actorId, long meetingId, RequestStatus status,
                                                         String cursor, int size) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!meetingAuthorization.canManage(actorId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingJoinRequestEntity> rows = meetingJoinRequestRepository.listForMeeting(
                meetingId, actorId, status,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<MeetingJoinRequestEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<JoinRequestResponse> items = page.stream().map(JoinRequestServiceImpl::toMeetingResponse).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getTimeSent(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional
    public JoinRequestResponse approveGroup(long actorId, long requestId) {
        GroupJoinRequestEntity request = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        GroupEntity group = groupRepository.findByIdForUpdate(request.getGroup().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (!groupAuthorization.isAdminOrOwner(actorId, group)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        if (groupAuthorization.isBanned(request.getRequester().getId(), group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requester is banned from this group");
        }
        respond(request, RequestStatus.APPROVED);
        addGroupMember(group, request.getRequester());
        notifyRequester(request.getRequester(), CATEGORY_GROUP, "GROUP_JOIN_REQUEST_APPROVED", Map.of(
                "name", request.getRequester().getUsername(),
                "group", group.getGroupName(),
                "link", appBaseUrl + "/groups/" + group.getId()));
        return toGroupResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse declineGroup(long actorId, long requestId) {
        GroupJoinRequestEntity request = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        GroupEntity group = request.getGroup();
        if (!groupAuthorization.isAdminOrOwner(actorId, group)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        respond(request, RequestStatus.DECLINED);
        notifyRequester(request.getRequester(), CATEGORY_GROUP, "GROUP_JOIN_REQUEST_DECLINED", Map.of(
                "name", request.getRequester().getUsername(),
                "group", group.getGroupName(),
                "link", appBaseUrl + "/groups/" + group.getId()));
        return toGroupResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse approveMeeting(long actorId, long requestId) {
        MeetingJoinRequestEntity request = meetingJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        MeetingEntity meeting = request.getMeeting();
        GroupEntity group = meeting.getGroup();
        if (group != null) {
            group = groupRepository.findByIdForUpdate(group.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        }
        // Locked after the group so approval serializes with meeting delete/removeParticipant.
        meeting = lockMeeting(meeting.getId());
        if (!meetingAuthorization.canManage(actorId, meeting)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        if (group != null && groupAuthorization.isBanned(request.getRequester().getId(), group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requester is banned from this group");
        }
        if (blockService.isBlocked(meeting.getCreator().getId(), request.getRequester().getId())) {
            throw new ResourceNotFoundException("Request not found");
        }
        boolean conflicts = !conflictService.findScheduleConflicts(
                request.getRequester().getId(), meeting.getStartTime(), meeting.getEndTime(), meeting.getId()).isEmpty();
        if (conflicts) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requester has a scheduling conflict");
        }
        respond(request, RequestStatus.APPROVED);
        addMeetingParticipant(meeting, request.getRequester());
        notifyRequester(request.getRequester(), CATEGORY_MEETING, "MEETING_JOIN_REQUEST_APPROVED", Map.of(
                "name", request.getRequester().getUsername(),
                "title", meeting.getTitle(),
                "link", appBaseUrl + "/meetings/" + meeting.getId()));
        return toMeetingResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse declineMeeting(long actorId, long requestId) {
        MeetingJoinRequestEntity request = meetingJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        MeetingEntity meeting = request.getMeeting();
        if (!meetingAuthorization.canManage(actorId, meeting)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        respond(request, RequestStatus.DECLINED);
        notifyRequester(request.getRequester(), CATEGORY_MEETING, "MEETING_JOIN_REQUEST_DECLINED", Map.of(
                "name", request.getRequester().getUsername(),
                "title", meeting.getTitle(),
                "link", appBaseUrl + "/meetings/" + meeting.getId()));
        return toMeetingResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse cancelGroup(long userId, long requestId) {
        GroupJoinRequestEntity request = groupJoinRequestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        requirePending(request);
        respond(request, RequestStatus.CANCELLED);
        return toGroupResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse cancelMeeting(long userId, long requestId) {
        MeetingJoinRequestEntity request = meetingJoinRequestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        requirePending(request);
        respond(request, RequestStatus.CANCELLED);
        return toMeetingResponse(request);
    }

    // Conditional UPDATE ... WHERE status = 'PENDING' so concurrent approve/decline/cancel calls can't both win.
    private void respond(GroupJoinRequestEntity request, RequestStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        if (groupJoinRequestRepository.transitionStatus(request.getId(), RequestStatus.PENDING, status, now) == 0) {
            throw alreadyResponded();
        }
        request.setStatus(status);
        request.setRespondedAt(now);
    }

    private void respond(MeetingJoinRequestEntity request, RequestStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        if (meetingJoinRequestRepository.transitionStatus(request.getId(), RequestStatus.PENDING, status, now) == 0) {
            throw alreadyResponded();
        }
        request.setStatus(status);
        request.setRespondedAt(now);
    }

    private void requirePending(GroupJoinRequestEntity request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw alreadyResponded();
        }
    }

    private void requirePending(MeetingJoinRequestEntity request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw alreadyResponded();
        }
    }

    private static ResponseStatusException alreadyPending() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "A join request is already pending");
    }

    private MeetingEntity lockMeeting(long meetingId) {
        return meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    private static ResponseStatusException alreadyResponded() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been responded to");
    }

    private void addGroupMember(GroupEntity group, UserEntity user) {
        if (group.getOwner().getId().equals(user.getId())
                || groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            return;
        }
        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(group);
        member.setUser(user);
        member.setAdmin(false);
        groupMemberRepository.save(member);
    }

    private void addMeetingParticipant(MeetingEntity meeting, UserEntity user) {
        if (!meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            MeetingParticipantEntity participant = new MeetingParticipantEntity();
            participant.setMeeting(meeting);
            participant.setUser(user);
            try {
                meetingParticipantRepository.saveAndFlush(participant);
            } catch (DataIntegrityViolationException e) {
                // Safety net only; the meeting lock already serializes participant inserts.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The requester is already a participant");
            }
        }
    }

    private void notifyGroupApprovers(GroupEntity group, UserEntity requester) {
        Map<Long, String> approvers = new LinkedHashMap<>();
        approvers.put(group.getOwner().getId(), group.getOwner().getUsername());
        for (GroupMemberEntity member : groupMemberRepository.findByGroupId(group.getId())) {
            if (member.isAdmin()) {
                approvers.putIfAbsent(member.getUser().getId(), member.getUser().getUsername());
            }
        }
        approvers.remove(requester.getId());
        for (Map.Entry<Long, String> approver : approvers.entrySet()) {
            notificationService.push(approver.getKey(), "GROUP_JOIN_REQUEST", CATEGORY_GROUP,
                    "GROUP_JOIN_REQUEST", Map.of(
                            "name", approver.getValue(),
                            "requester", requester.getUsername(),
                            "group", group.getGroupName(),
                            "link", appBaseUrl + "/groups/" + group.getId()));
        }
    }

    private void notifyMeetingApprovers(MeetingEntity meeting, UserEntity requester) {
        Map<Long, String> approvers = new LinkedHashMap<>();
        approvers.put(meeting.getCreator().getId(), meeting.getCreator().getUsername());
        GroupEntity group = meeting.getGroup();
        if (group != null) {
            approvers.putIfAbsent(group.getOwner().getId(), group.getOwner().getUsername());
            for (GroupMemberEntity member : groupMemberRepository.findByGroupId(group.getId())) {
                if (member.isAdmin()) {
                    approvers.putIfAbsent(member.getUser().getId(), member.getUser().getUsername());
                }
            }
        }
        approvers.remove(requester.getId());
        for (Map.Entry<Long, String> approver : approvers.entrySet()) {
            notificationService.push(approver.getKey(), "MEETING_JOIN_REQUEST", CATEGORY_MEETING,
                    "MEETING_JOIN_REQUEST", Map.of(
                            "name", approver.getValue(),
                            "requester", requester.getUsername(),
                            "title", meeting.getTitle(),
                            "link", appBaseUrl + "/meetings/" + meeting.getId()));
        }
    }

    private void notifyRequester(UserEntity requester, String category, String type, Map<String, String> vars) {
        notificationService.push(requester.getId(), type, category, type, vars);
    }

    private static JoinRequestResponse toGroupResponse(GroupJoinRequestEntity request) {
        UserEntity requester = request.getRequester();
        return new JoinRequestResponse(
                request.getId(),
                KIND_GROUP,
                request.getStatus(),
                requester.getId(),
                requester.getUsername(),
                request.getGroup() == null ? null : request.getGroup().getId(),
                request.getGroup() == null ? null : request.getGroup().getGroupName(),
                null,
                null,
                request.getTimeSent(),
                request.getRespondedAt());
    }

    private static JoinRequestResponse toMeetingResponse(MeetingJoinRequestEntity request) {
        UserEntity requester = request.getRequester();
        return new JoinRequestResponse(
                request.getId(),
                KIND_MEETING,
                request.getStatus(),
                requester.getId(),
                requester.getUsername(),
                null,
                null,
                request.getMeeting() == null ? null : request.getMeeting().getId(),
                request.getMeeting() == null ? null : request.getMeeting().getTitle(),
                request.getTimeSent(),
                request.getRespondedAt());
    }
}
