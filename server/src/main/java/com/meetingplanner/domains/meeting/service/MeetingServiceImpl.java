package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingInviteRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.security.InviteTokens;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MeetingServiceImpl implements MeetingService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final MeetingAuthorizationService authorization;
    private final GroupAuthorizationService groupAuthorization;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public MeetingServiceImpl(MeetingAuthorizationService authorization,
                              GroupAuthorizationService groupAuthorization,
                              MeetingRepository meetingRepository,
                              MeetingParticipantRepository meetingParticipantRepository,
                              MeetingInviteRepository meetingInviteRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.authorization = authorization;
        this.groupAuthorization = groupAuthorization;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public MeetingResponse create(long userId, MeetingCreateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse get(long userId, long meetingId) {
        if (!authorization.canView(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request) {
        if (!authorization.canManage(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void delete(long userId, long meetingId) {
        if (!authorization.canManage(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listMine(long userId, Long groupId, String cursor, int size) {
        int limit = clampSize(size);
        Cursor decoded = decodeCursor(cursor);
        List<MeetingEntity> rows = meetingRepository.listMine(
                userId, groupId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MeetingSummaryResponse> listCreated(long userId, String cursor, int size) {
        int limit = clampSize(size);
        Cursor decoded = decodeCursor(cursor);
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
        if (!groupAuthorization.isOwner(userId, groupId) && !groupAuthorization.isMember(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        int limit = clampSize(size);
        Cursor decoded = decodeCursor(cursor);
        List<MeetingEntity> rows = meetingRepository.listGroup(
                groupId, userId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));
        return toPage(rows, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> listUpcoming(long userId) {
        List<MeetingEntity> meetings = meetingRepository.findTop10ByParticipants_User_IdAndStartTimeAfterOrderByStartTimeAsc(
                userId, OffsetDateTime.now());

        List<MeetingResponse> responseList = new ArrayList<>();
        for (MeetingEntity m : meetings) {
            List<ParticipantResponse> participantResponseList = new ArrayList<>();

            Set<MeetingParticipantEntity> participants = m.getParticipants();

            for (MeetingParticipantEntity p : participants) {
                ParticipantResponse participantResponse = new ParticipantResponse(
                        p.getUser().getId(),
                        p.getUser().getUsername());
                participantResponseList.add(participantResponse);
            }

            MeetingResponse response = new MeetingResponse(
                    m.getId(),
                    m.getGroup().getId(),
                    m.getCreator().getId(),
                    m.getStartTime(),
                    m.getEndTime(),
                    m.getLocation(),
                    m.getDescription(),
                    m.isPrivate(),
                    m.getExternalParticipantNames(),
                    participantResponseList);
            responseList.add(response);
        }
        return responseList;
    }

    private CursorPage<MeetingSummaryResponse> toPage(List<MeetingEntity> rows, int size) {
        boolean hasMore = rows.size() > size;
        List<MeetingEntity> page = hasMore ? rows.subList(0, size) : rows;
        Map<Long, Long> counts = countsByMeetingId(page);
        List<MeetingSummaryResponse> items = page.stream()
                .map(m -> toSummary(m, counts.getOrDefault(m.getId(), 0L)))
                .toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? encodeCursor(page.getLast())
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
                m.getDescription(),
                m.isPrivate(),
                (int) participants + external);
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String encodeCursor(MeetingEntity m) {
        String raw = m.getStartTime() + "|" + m.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            return new Cursor(OffsetDateTime.parse(raw.substring(0, sep)), Long.parseLong(raw.substring(sep + 1)));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private record Cursor(OffsetDateTime time, long id) {
    }

    @Override
    @Transactional
    public ParticipantResponse addParticipant(long actorUserId, long meetingId, MemberUsernameRequest request) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void removeParticipant(long actorUserId, long meetingId, long targetUserId) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public InviteResponse invite(long actorUserId, long meetingId, MeetingInviteRequest request) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }

        if (request.hasUsername() == request.hasEmail()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either username or email");
        }

        UserEntity recipient = null;
        String recipientEmail;
        if (request.hasUsername()) {
            recipient = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (recipient.getEmailVerifiedAt() == null) {
                throw new EmailNotVerifiedException();
            }
            recipientEmail = recipient.getEmail();
        } else {
            recipientEmail = request.email().trim();
            recipient = userRepository.findByEmail(recipientEmail)
                    .filter(u -> u.getEmailVerifiedAt() != null)
                    .orElse(null);
        }

        MeetingInviteEntity invite = meetingInviteRepository
                .findByMeetingIdAndRecipientEmail(meetingId, recipientEmail)
                .map(this::reuseForReinvite)
                .orElseGet(MeetingInviteEntity::new);

        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        UserEntity sender = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String rawToken = InviteTokens.generateRawToken();
        invite.setSender(sender);
        invite.setMeeting(meeting);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipientEmail);
        invite.setTokenHash(InviteTokens.hash(rawToken));
        invite.setStatus(InviteStatus.PENDING);
        invite.setRespondedAt(null);
        meetingInviteRepository.save(invite);

        String link = appBaseUrl + "/invites/meeting?token=" + rawToken;
        notificationService.queueEmailTo(recipientEmail, "MEETING_INVITE", Map.of(
                "name", recipient != null ? recipient.getUsername() : emailDisplayName(recipientEmail),
                "sender", sender.getUsername(),
                "link", link));

        return toInviteResponse(invite);
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

    private static InviteResponse toInviteResponse(MeetingInviteEntity invite) {
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