package com.meetingplanner.domains.request.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.RequestStatus;
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
import org.springframework.data.domain.PageRequest;
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
                                  NotificationService notificationService) {
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
    }

    @Override
    @Transactional
    public JoinRequestResponse requestGroupJoin(long userId, long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!group.isPrivate()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This group is open to join");
        }
        if (groupAuthorization.isOwner(userId, group) || groupAuthorization.isMember(userId, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group");
        }
        if (groupJoinRequestRepository.existsByGroupIdAndRequesterIdAndStatus(groupId, userId, RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A join request is already pending");
        }

        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GroupJoinRequestEntity request = new GroupJoinRequestEntity();
        request.setRequester(requester);
        request.setGroup(group);
        request.setStatus(RequestStatus.PENDING);
        request.setTimeSent(OffsetDateTime.now());
        groupJoinRequestRepository.save(request);

        notifyGroupApprovers(group, requester);

        return toGroupResponse(request);
    }

    @Override
    @Transactional
    public JoinRequestResponse requestMeetingJoin(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        GroupEntity group = meeting.getGroup();
        if (meeting.isPrivate() || group == null || group.isPrivate()) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        if (meetingAuthorization.isParticipantOrCreator(userId, meeting)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a participant of this meeting");
        }
        if (meetingJoinRequestRepository.existsByMeetingIdAndRequesterIdAndStatus(
                meetingId, userId, RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A join request is already pending");
        }

        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MeetingJoinRequestEntity request = new MeetingJoinRequestEntity();
        request.setRequester(requester);
        request.setMeeting(meeting);
        request.setStatus(RequestStatus.PENDING);
        request.setTimeSent(OffsetDateTime.now());
        meetingJoinRequestRepository.save(request);

        notifyMeetingApprovers(meeting, requester);

        return toMeetingResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<JoinRequestResponse> listMine(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        String cursorKind = decoded == null ? null : decoded.kind();
        Long cursorId = decoded == null ? null : decoded.id();

        List<GroupJoinRequestEntity> groupRows = groupJoinRequestRepository.listForRequester(
                userId, KIND_GROUP, cursorTime, cursorKind, cursorId, PageRequest.of(0, limit + 1));
        List<MeetingJoinRequestEntity> meetingRows = meetingJoinRequestRepository.listForRequester(
                userId, KIND_MEETING, cursorTime, cursorKind, cursorId, PageRequest.of(0, limit + 1));

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
    public CursorPage<JoinRequestResponse> listForGroup(long actorId, long groupId, String cursor, int size) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!groupAuthorization.isAdminOrOwner(actorId, group)) {
            throw new ResourceNotFoundException("Group not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupJoinRequestEntity> rows = groupJoinRequestRepository.listForGroup(
                groupId,
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
    public CursorPage<JoinRequestResponse> listForMeeting(long actorId, long meetingId, String cursor, int size) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!meetingAuthorization.canManage(actorId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingJoinRequestEntity> rows = meetingJoinRequestRepository.listForMeeting(
                meetingId,
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
        GroupEntity group = request.getGroup();
        if (!groupAuthorization.isAdminOrOwner(actorId, group)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        addGroupMember(group, request.getRequester());
        respond(request, RequestStatus.APPROVED);
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
        if (!meetingAuthorization.canManage(actorId, meeting)) {
            throw new ResourceNotFoundException("Request not found");
        }
        requirePending(request);
        boolean conflicts = !conflictService.findScheduleConflicts(
                request.getRequester().getId(), meeting.getStartTime(), meeting.getEndTime(), meeting.getId()).isEmpty();
        if (conflicts) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requester has a scheduling conflict");
        }
        addMeetingParticipant(meeting, request.getRequester());
        respond(request, RequestStatus.APPROVED);
        notifyRequester(request.getRequester(), CATEGORY_MEETING, "MEETING_JOIN_REQUEST_APPROVED", Map.of(
                "name", request.getRequester().getUsername(),
                "title", meetingTitle(meeting),
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
                "title", meetingTitle(meeting),
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

    private void respond(GroupJoinRequestEntity request, RequestStatus status) {
        request.setStatus(status);
        request.setRespondedAt(OffsetDateTime.now());
        groupJoinRequestRepository.save(request);
    }

    private void respond(MeetingJoinRequestEntity request, RequestStatus status) {
        request.setStatus(status);
        request.setRespondedAt(OffsetDateTime.now());
        meetingJoinRequestRepository.save(request);
    }

    private void requirePending(GroupJoinRequestEntity request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been responded to");
        }
    }

    private void requirePending(MeetingJoinRequestEntity request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been responded to");
        }
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
            meetingParticipantRepository.save(participant);
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
                            "title", meetingTitle(meeting),
                            "link", appBaseUrl + "/meetings/" + meeting.getId()));
        }
    }

    private void notifyRequester(UserEntity requester, String category, String type, Map<String, String> vars) {
        notificationService.push(requester.getId(), type, category, type, vars);
    }

    private static String meetingTitle(MeetingEntity meeting) {
        return meeting.getDescription() != null && !meeting.getDescription().isBlank()
                ? meeting.getDescription()
                : "your meeting";
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
                request.getMeeting() == null ? null : request.getMeeting().getId(),
                request.getTimeSent(),
                request.getRespondedAt());
    }
}
