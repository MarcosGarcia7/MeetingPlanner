package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.SearchQuery;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.InviteCheckResponse;
import com.meetingplanner.common.api.dto.InviteCheckResult;
import com.meetingplanner.common.api.dto.InviteFailure;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingInviteRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.ForbiddenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.UserProfileAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingAuthorizationService authorization;
    private final GroupAuthorizationService groupAuthorization;
    private final GroupRepository groupRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final UserRepository userRepository;
    private final UserProfileAuthorizationService userProfileAuthorization;
    private final NotificationService notificationService;
    private final MeetingConflictService conflictService;
    private final MeetingTimeChangeService timeChangeService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Value("${app.meeting.start-grace-minutes:5}")
    private long startGraceMinutes;

    public MeetingServiceImpl(MeetingAuthorizationService authorization,
                              GroupAuthorizationService groupAuthorization,
                              GroupRepository groupRepository,
                              MeetingRepository meetingRepository,
                              MeetingParticipantRepository meetingParticipantRepository,
                              MeetingInviteRepository meetingInviteRepository,
                              UserRepository userRepository,
                              UserProfileAuthorizationService userProfileAuthorization,
                              NotificationService notificationService,
                              MeetingConflictService conflictService,
                              MeetingTimeChangeService timeChangeService) {
        this.authorization = authorization;
        this.groupAuthorization = groupAuthorization;
        this.groupRepository = groupRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.userRepository = userRepository;
        this.userProfileAuthorization = userProfileAuthorization;
        this.notificationService = notificationService;
        this.conflictService = conflictService;
        this.timeChangeService = timeChangeService;
    }

    @Override
    @Transactional
    public MeetingResponse create(long userId, MeetingCreateRequest request) {
        return doCreate(userId, null, request);
    }

    @Override
    @Transactional
    public MeetingResponse createInGroup(long userId, long groupId, MeetingCreateRequest request) {
        GroupEntity group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        boolean allowed = group.isMembersCanCreate()
                ? groupAuthorization.isOwner(userId, group) || groupAuthorization.isMember(userId, group)
                : groupAuthorization.isAdminOrOwner(userId, group);
        if (!allowed) {
            throw new ForbiddenException("You cannot create meetings in this group");
        }
        return doCreate(userId, group, request);
    }

    private MeetingResponse doCreate(long userId, GroupEntity group, MeetingCreateRequest request) {
        UserEntity creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateTimes(request.startTime(), request.endTime());

        List<MeetingEntity> conflicts = conflictService.findScheduleConflicts(
                userId, request.startTime(), request.endTime(), null);
        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, scheduleConflictMessage(conflicts));
        }

        MeetingEntity meeting = new MeetingEntity();
        meeting.setGroup(group);
        meeting.setCreator(creator);
        meeting.setStartTime(request.startTime());
        meeting.setEndTime(request.endTime());
        meeting.setLocation(request.location());
        meeting.setTitle(request.title());
        meeting.setPrivate(Boolean.TRUE.equals(request.isPrivate()));
        meeting.addParticipant(creator);

        MeetingEntity saved = meetingRepository.save(meeting);
        List<String> warnings = conflictService.availabilityWarnings(userId, saved.getStartTime(), saved.getEndTime());
        return toResponse(saved, warnings);
    }

    private void validateTimes(OffsetDateTime start, OffsetDateTime end) {
        if (end != null && !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        if (start.isBefore(OffsetDateTime.now().minusMinutes(startGraceMinutes))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime cannot be in the past");
        }
    }

    private static String scheduleConflictMessage(List<MeetingEntity> conflicts) {
        MeetingEntity first = conflicts.getFirst();
        return "Conflicts with an existing meeting: \"" + first.getTitle() + "\" at "
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(first.getStartTime());
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse get(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canView(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        return toResponse(meeting);
    }

    @Override
    @Transactional
    public MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canManage(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        boolean startChanging = request.startTime() != null && !Objects.equals(request.startTime(), meeting.getStartTime());
        boolean endChanging = request.endTime() != null && !Objects.equals(request.endTime(), meeting.getEndTime());
        boolean timesChanging = startChanging || endChanging;
        OffsetDateTime newStart = startChanging ? request.startTime() : meeting.getStartTime();
        OffsetDateTime newEnd = endChanging ? request.endTime() : meeting.getEndTime();
        if (timesChanging) {
            validateTimes(newStart, newEnd);
        }

        boolean nonTimeChanged = false;
        if (request.location() != null && !Objects.equals(request.location(), meeting.getLocation())) {
            meeting.setLocation(request.location());
            nonTimeChanged = true;
        }
        if (request.title() != null && !Objects.equals(request.title(), meeting.getTitle())) {
            meeting.setTitle(request.title());
            nonTimeChanged = true;
        }
        if (request.isPrivate() != null && !Objects.equals(request.isPrivate(), meeting.isPrivate())) {
            meeting.setPrivate(request.isPrivate());
            nonTimeChanged = true;
        }

        if (timesChanging && hasOtherParticipants(meeting, userId)) {
            if (timeChangeService.hasPendingProposal(meetingId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A time change is already pending for this meeting");
            }
            meetingRepository.save(meeting);
            if (nonTimeChanged) {
                notifyParticipants(meeting, userId, "MEETING_UPDATED", notificationVars(meeting));
            }
            timeChangeService.createProposal(userId, meeting, newStart, newEnd);
            return toResponse(meeting);
        }

        boolean changed = false;
        if (startChanging) {
            meeting.setStartTime(request.startTime());
            changed = true;
        }
        if (endChanging) {
            meeting.setEndTime(request.endTime());
            changed = true;
        }
        if (nonTimeChanged) {
            changed = true;
        }

        MeetingEntity saved = meetingRepository.save(meeting);
        if (changed) {
            notifyParticipants(saved, userId, "MEETING_UPDATED", notificationVars(saved));
        }
        return toResponse(saved);
    }

    private static boolean hasOtherParticipants(MeetingEntity meeting, long actorUserId) {
        for (MeetingParticipantEntity p : meeting.getParticipants()) {
            if (!p.getUser().getId().equals(actorUserId)) {
                return true;
            }
        }
        return meeting.getCreator() != null && !meeting.getCreator().getId().equals(actorUserId);
    }

    @Override
    @Transactional
    public void delete(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canManage(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        notifyParticipants(meeting, userId, "MEETING_DELETED", notificationVars(meeting));
        meetingRepository.delete(meeting);
    }

    private void notifyParticipants(MeetingEntity meeting, long actorUserId, String type, Map<String, String> vars) {
        Map<Long, UserEntity> recipients = new LinkedHashMap<>();
        for (MeetingParticipantEntity p : meeting.getParticipants()) {
            recipients.putIfAbsent(p.getUser().getId(), p.getUser());
        }
        if (meeting.getCreator() != null) {
            recipients.putIfAbsent(meeting.getCreator().getId(), meeting.getCreator());
        }
        recipients.remove(actorUserId);
        for (UserEntity recipient : recipients.values()) {
            Map<String, String> perUser = new HashMap<>(vars);
            perUser.put("name", displayName(recipient));
            notificationService.push(recipient.getId(), type, "meeting_updates", type, perUser);
        }
    }

    private Map<String, String> notificationVars(MeetingEntity meeting) {
        Map<String, String> vars = new HashMap<>();
        vars.put("title", meeting.getTitle());
        vars.put("start_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(meeting.getStartTime()));
        vars.put("location", meeting.getLocation() == null ? "" : meeting.getLocation());
        vars.put("link", appBaseUrl + "/meetings/" + meeting.getId());
        return vars;
    }

    private static String displayName(UserEntity user) {
        return user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : user.getUsername();
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listMine(long userId, Long groupId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingEntity> rows = meetingRepository.listMine(
                userId, groupId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> search(long viewerId, String query, String cursor, int size) {
        SearchQuery search = SearchQuery.parse(query);
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingEntity> rows = meetingRepository.searchVisible(
                viewerId,
                search.pattern(),
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listCreated(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingEntity> rows = meetingRepository.listCreated(
                userId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listForGroup(long userId, long groupId, String cursor, int size) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        boolean member = groupAuthorization.isOwner(userId, group) || groupAuthorization.isMember(userId, group);
        if (!member && group.isPrivate()) {
            throw new ResourceNotFoundException("Group not found");
        }
        boolean canSeePrivate = groupAuthorization.isOwner(userId, group) || groupAuthorization.isAdmin(userId, group);
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingEntity> rows = meetingRepository.listGroup(
                groupId, userId, canSeePrivate,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listSchedule(long viewerId, long targetUserId, String cursor, int size) {
        boolean own = viewerId == targetUserId;
        if (!own && !userProfileAuthorization.canViewSchedule(viewerId, targetUserId)) {
            throw new ResourceNotFoundException("User not found");
        }
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<MeetingEntity> rows = meetingRepository.listForUser(
                targetUserId, !own,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingSummaryResponse> listUpcoming(long userId) {
        List<MeetingEntity> rows =  meetingRepository.findTop10ByParticipants_User_IdAndStartTimeAfterOrderByStartTimeAsc(userId, OffsetDateTime.now());

        List<MeetingSummaryResponse> responseList = new ArrayList<>();

        Map<Long, Long> counts = countsByMeetingId(rows);
        for (MeetingEntity m : rows) {
            responseList.add(toSummary(m, counts.getOrDefault(m.getId(), 0L)));
        }

        return responseList;
    }

    private MeetingResponse toResponse(MeetingEntity m) {
        return toResponse(m, List.of());
    }

    private MeetingResponse toResponse(MeetingEntity m, List<String> warnings) {
        List<ParticipantResponse> participants = new ArrayList<>();
        for (MeetingParticipantEntity p : m.getParticipants()) {
            participants.add(new ParticipantResponse(p.getUser().getId(), p.getUser().getUsername()));
        }
        return new MeetingResponse(
                m.getId(),
                m.getGroup() == null ? null : m.getGroup().getId(),
                m.getCreator().getId(),
                m.getStartTime(),
                m.getEndTime(),
                m.getLocation(),
                m.getTitle(),
                m.isPrivate(),
                m.getExternalParticipantNames(),
                participants,
                warnings,
                timeChangeService.pendingSummary(m.getId()).orElse(null));
    }

    private CursorPage<MeetingSummaryResponse> toPage(List<MeetingEntity> rows, int size) {
        boolean hasMore = rows.size() > size;
        List<MeetingEntity> page = hasMore ? rows.subList(0, size) : rows;
        Map<Long, Long> counts = countsByMeetingId(page);
        List<MeetingSummaryResponse> items = page.stream()
                .map(m -> toSummary(m, counts.getOrDefault(m.getId(), 0L)))
                .toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getStartTime(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private Map<Long, Long> countsByMeetingId(List<MeetingEntity> meetings) {
        if (meetings.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = meetings.stream().map(MeetingEntity::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : meetingParticipantRepository.countByMeetingIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private MeetingSummaryResponse toSummary(MeetingEntity m, long participants) {
        int external = m.getExternalParticipantNames() == null ? 0 : m.getExternalParticipantNames().size();
        return new MeetingSummaryResponse(
                m.getId(),
                m.getGroup() == null ? null : m.getGroup().getId(),
                m.getCreator().getId(),
                m.getStartTime(),
                m.getEndTime(),
                m.getLocation(),
                m.getTitle(),
                m.isPrivate(),
                (int) participants + external);
    }

    @Override
    @Transactional
    public void removeParticipant(long actorUserId, long meetingId, long targetUserId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canManage(actorUserId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        meetingParticipantRepository.deleteByMeetingIdAndUserId(meetingId, targetUserId);
    }

    @Override
    @Transactional
    public void leaveMeeting(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (meeting.getCreator() != null && meeting.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The meeting creator cannot leave the meeting");
        }
        if (!authorization.canView(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        meetingParticipantRepository.deleteByMeetingIdAndUserId(meetingId, userId);
    }

    @Override
    @Transactional
    public InviteBatchResponse inviteBatch(long actorUserId, long meetingId, MeetingInviteBatchRequest request) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canManage(actorUserId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        UserEntity sender = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<InviteResponse> invited = new ArrayList<>();
        List<InviteFailure> failed = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (MeetingInviteRequest recipient : request.recipients()) {
            String dedupeKey = dedupeKey(recipient);
            if (dedupeKey != null && !seen.add(dedupeKey)) {
                failed.add(failure(recipient, "Duplicate recipient in request"));
                continue;
            }
            try {
                invited.add(inviteOne(meeting, sender, recipient));
            } catch (ResourceNotFoundException | EmailNotVerifiedException | ResponseStatusException e) {
                failed.add(failure(recipient, reasonFor(e)));
            }
        }

        if (invited.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invites could be sent");
        }

        return new InviteBatchResponse(invited, failed);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteCheckResponse checkInvites(long actorUserId, long meetingId, MeetingInviteBatchRequest request) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canManage(actorUserId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        UserEntity sender = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<InviteCheckResult> results = new ArrayList<>();
        for (MeetingInviteRequest recipient : request.recipients()) {
            UserEntity target = resolveRecipient(recipient);
            List<String> warnings = target != null
                    ? recipientWarnings(sender, target, meeting)
                    : List.of();
            results.add(new InviteCheckResult(recipient.username(), recipient.email(), warnings));
        }
        return new InviteCheckResponse(results);
    }

    private UserEntity resolveRecipient(MeetingInviteRequest recipient) {
        if (recipient.hasUsername()) {
            return userRepository.findByUsername(recipient.username()).orElse(null);
        }
        if (recipient.hasEmail()) {
            return userRepository.findByEmail(recipient.email().trim())
                    .filter(u -> u.getEmailVerifiedAt() != null)
                    .orElse(null);
        }
        return null;
    }

    private InviteResponse inviteOne(MeetingEntity meeting, UserEntity sender, MeetingInviteRequest request) {
        if (request.hasUsername() == request.hasEmail()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either username or email");
        }

        UserEntity recipient = null;
        String recipientEmail;
        if (request.hasUsername()) {
            recipient = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (recipient.getId().equals(sender.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
            }
            if (recipient.getEmailVerifiedAt() == null) {
                throw new EmailNotVerifiedException();
            }
            recipientEmail = recipient.getEmail();
        } else {
            recipientEmail = request.email().trim();
            recipient = userRepository.findByEmail(recipientEmail)
                    .filter(u -> u.getEmailVerifiedAt() != null)
                    .orElse(null);
            if (recipient != null && recipient.getId().equals(sender.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
            }
        }

        MeetingInviteEntity invite = meetingInviteRepository
                .findByMeetingIdAndRecipientEmail(meeting.getId(), recipientEmail)
                .map(this::reuseForReinvite)
                .orElseGet(MeetingInviteEntity::new);

        String rawToken = InviteTokens.generateRawToken();
        invite.setSender(sender);
        invite.setMeeting(meeting);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipientEmail);
        invite.setTokenHash(InviteTokens.hash(rawToken));
        invite.setStatus(InviteStatus.PENDING);
        invite.setRespondedAt(null);
        invite.setTimeSent(OffsetDateTime.now());
        meetingInviteRepository.save(invite);

        String link = appBaseUrl + "/invites/meeting?token=" + rawToken;
        String details = "Starts: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(meeting.getStartTime());
        if (meeting.getEndTime() != null) {
            details += "\nEnds: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(meeting.getEndTime());
        }
        if (meeting.getLocation() != null && !meeting.getLocation().isBlank()) {
            details += "\nLocation: " + meeting.getLocation();
        }
        notificationService.queueEmailToForUser(recipient, recipientEmail, "MEETING_INVITE", Map.of(
                "name", recipient != null ? recipient.getUsername() : emailDisplayName(recipientEmail),
                "sender", sender.getUsername(),
                "title", meeting.getTitle(),
                "details", details,
                "link", link), "meeting_invites");

        return toInviteResponse(invite, recipientWarnings(sender, recipient, meeting));
    }

    private List<String> recipientWarnings(UserEntity sender, UserEntity recipient, MeetingEntity meeting) {
        if (recipient == null) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        warnings.addAll(conflictService.scheduleWarningsForViewer(
                sender.getId(), recipient.getId(), meeting.getStartTime(), meeting.getEndTime()));
        warnings.addAll(conflictService.availabilityWarningsForViewer(
                sender.getId(), recipient.getId(), meeting.getStartTime(), meeting.getEndTime()));
        return warnings;
    }

    private static String dedupeKey(MeetingInviteRequest recipient) {
        if (recipient.hasUsername()) {
            return "u:" + recipient.username().toLowerCase();
        }
        if (recipient.hasEmail()) {
            return "e:" + recipient.email().trim().toLowerCase();
        }
        return null;
    }

    private static InviteFailure failure(MeetingInviteRequest recipient, String reason) {
        return new InviteFailure(recipient.username(), recipient.email(), reason);
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

    private MeetingInviteEntity reuseForReinvite(MeetingInviteEntity existing) {
        if (existing.getStatus() == InviteStatus.PENDING || existing.getStatus() == InviteStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invite already exists for this email");
        }
        return existing;
    }

    private static String emailDisplayName(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private static InviteResponse toInviteResponse(MeetingInviteEntity invite, List<String> warnings) {
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
                invite.getMeeting() == null ? null : invite.getMeeting().getId(),
                warnings);
    }
}