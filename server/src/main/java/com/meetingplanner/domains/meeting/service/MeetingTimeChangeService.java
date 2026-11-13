package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.MeetingTimes;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.PendingTimeChange;
import com.meetingplanner.common.api.dto.ScheduleConflict;
import com.meetingplanner.common.api.dto.TimeChangeProposalResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.ProposalRole;
import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.common.api.model.VoteStatus;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeProposalEntity;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeVoteEntity;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.repository.MeetingTimeChangeProposalRepository;
import com.meetingplanner.domains.meeting.repository.MeetingTimeChangeVoteRepository;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Unanimous reschedule voting for meeting time changes.
 *
 * <p>When a time change is requested on a meeting with other participants, a proposal is created
 * and every participant (except the proposer, who is treated as approving) must approve it. Any
 * rejection fails the proposal, and unresponsive proposals expire.
 */
@Service
public class MeetingTimeChangeService {

    private static final String CATEGORY = "meeting_updates";
    private static final int AWAITING_VOTE_LIMIT = 20;

    private final MeetingRepository meetingRepository;
    private final MeetingTimeChangeProposalRepository proposalRepository;
    private final MeetingTimeChangeVoteRepository voteRepository;
    private final MeetingConflictService conflictService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final MeetingAuthorizationService authorization;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Value("${app.meeting.time-change-deadline-hours:48}")
    private long deadlineHours;

    // Time zone for users without a profile zone (and non-users), from DEFAULT_TIMEZONE.
    @Value("${app.default-timezone:UTC}")
    private String defaultTimezone;

    public MeetingTimeChangeService(MeetingRepository meetingRepository,
                                    MeetingTimeChangeProposalRepository proposalRepository,
                                    MeetingTimeChangeVoteRepository voteRepository,
                                    MeetingConflictService conflictService,
                                    NotificationService notificationService,
                                    UserRepository userRepository,
                                    MeetingAuthorizationService authorization) {
        this.meetingRepository = meetingRepository;
        this.proposalRepository = proposalRepository;
        this.voteRepository = voteRepository;
        this.conflictService = conflictService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public Optional<PendingTimeChange> pendingSummary(long meetingId) {
        return proposalRepository.findByMeetingIdAndStatus(meetingId, ProposalStatus.PENDING)
                .map(this::toPendingSummary);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingProposal(long meetingId) {
        return proposalRepository.findByMeetingIdAndStatus(meetingId, ProposalStatus.PENDING).isPresent();
    }

    @Transactional
    public TimeChangeProposalResponse createProposal(long actorUserId, MeetingEntity meeting,
                                                     OffsetDateTime start, OffsetDateTime end) {
        if (hasPendingProposal(meeting.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A time change is already pending for this meeting");
        }
        UserEntity proposer = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MeetingTimeChangeProposalEntity proposal = new MeetingTimeChangeProposalEntity();
        proposal.setMeeting(meeting);
        proposal.setProposedBy(proposer);
        proposal.setProposedStartTime(start);
        proposal.setProposedEndTime(end);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setTimeSent(OffsetDateTime.now());
        try {
            proposalRepository.saveAndFlush(proposal);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A time change is already pending for this meeting");
        }

        for (UserEntity participant : recipientsOf(meeting).values()) {
            if (participant.getId().equals(actorUserId)) {
                continue;
            }
            MeetingTimeChangeVoteEntity vote = new MeetingTimeChangeVoteEntity();
            vote.setProposal(proposal);
            vote.setUser(participant);
            vote.setStatus(VoteStatus.PENDING);
            voteRepository.save(vote);
        }

        notifyParticipants(meeting, actorUserId, "MEETING_TIME_CHANGE_REQUEST",
                zone -> requestVars(meeting, proposal, zone));
        return toResponse(proposal, actorUserId);
    }

    @Transactional(readOnly = true)
    public TimeChangeProposalResponse getProposal(long userId, long meetingId) {
        MeetingEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        if (!authorization.canView(userId, meeting)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        MeetingTimeChangeProposalEntity proposal = proposalRepository
                .findByMeetingIdAndStatus(meetingId, ProposalStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("No pending time change"));
        return toResponse(proposal, userId);
    }

    @Transactional
    public TimeChangeProposalResponse vote(long userId, long meetingId, long proposalId, boolean approve) {
        lockMeeting(meetingId);
        MeetingTimeChangeProposalEntity proposal = proposalRepository.findByIdAndMeetingId(proposalId, meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This proposal has already been resolved");
        }
        MeetingTimeChangeVoteEntity vote = voteRepository.findByProposalIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (vote.getStatus() != VoteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already voted on this proposal");
        }
        MeetingEntity meeting = proposal.getMeeting();
        if (approve) {
            // Approving would double-book the voter; they must decline or clear the clash first.
            conflictService.assertNoScheduleConflict(userId, proposal.getProposedStartTime(),
                    proposal.getProposedEndTime(), meeting.getId());
        }

        vote.setStatus(approve ? VoteStatus.APPROVED : VoteStatus.DECLINED);
        vote.setRespondedAt(OffsetDateTime.now());
        voteRepository.save(vote);

        if (!approve) {
            resolve(proposal, ProposalStatus.REJECTED, meeting);
        } else {
            finalizeIfComplete(proposal, meeting);
        }
        return toResponse(proposal, userId);
    }

    /**
     * A participant left or was removed from the meeting (caller holds the meeting lock): their pending
     * vote no longer counts, which may complete the proposal for everyone else.
     */
    @Transactional
    public void onParticipantRemoved(long meetingId, long userId) {
        proposalRepository.findByMeetingIdAndStatus(meetingId, ProposalStatus.PENDING).ifPresent(proposal -> {
            if (voteRepository.deleteByProposalIdAndUserIdAndStatus(proposal.getId(), userId, VoteStatus.PENDING) > 0) {
                finalizeIfComplete(proposal, proposal.getMeeting());
            }
        });
    }

    // Once nobody is left to vote, applies the times, unless someone (e.g. an earlier approver who has since
    // joined another meeting) would now be double-booked, in which case the proposal is rejected instead.
    private void finalizeIfComplete(MeetingTimeChangeProposalEntity proposal, MeetingEntity meeting) {
        if (voteRepository.countByProposalIdAndStatus(proposal.getId(), VoteStatus.PENDING) > 0) {
            return;
        }
        boolean anyoneClashes = recipientsOf(meeting).keySet().stream()
                .anyMatch(id -> !conflictService.findScheduleConflicts(id, proposal.getProposedStartTime(),
                        proposal.getProposedEndTime(), meeting.getId()).isEmpty());
        if (anyoneClashes) {
            resolve(proposal, ProposalStatus.REJECTED, meeting);
            return;
        }
        applyTimes(meeting, proposal);
        resolve(proposal, ProposalStatus.APPROVED, meeting);
    }

    @Transactional
    public TimeChangeProposalResponse cancel(long userId, long meetingId, long proposalId) {
        lockMeeting(meetingId);
        MeetingTimeChangeProposalEntity proposal = proposalRepository.findByIdAndMeetingId(proposalId, meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This proposal has already been resolved");
        }
        if (!proposal.getProposedBy().getId().equals(userId)) {
            throw new ResourceNotFoundException("Proposal not found");
        }
        resolve(proposal, ProposalStatus.CANCELLED, proposal.getMeeting());
        return toResponse(proposal, userId);
    }

    @Transactional
    public int expireStale() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(deadlineHours);
        List<MeetingTimeChangeProposalEntity> stale =
                proposalRepository.findByStatusAndTimeSentBefore(ProposalStatus.PENDING, cutoff);
        int expired = 0;
        for (MeetingTimeChangeProposalEntity proposal : stale) {
            // No meeting lock here; the conditional transition skips proposals a concurrent vote/cancel resolved.
            if (tryResolve(proposal, ProposalStatus.EXPIRED, proposal.getMeeting())) {
                expired++;
            }
        }
        return expired;
    }

    private void lockMeeting(long meetingId) {
        meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
    }

    private void applyTimes(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal) {
        meeting.setStartTime(proposal.getProposedStartTime());
        meeting.setEndTime(proposal.getProposedEndTime());
        meetingRepository.save(meeting);
    }

    private void resolve(MeetingTimeChangeProposalEntity proposal, ProposalStatus status, MeetingEntity meeting) {
        if (!tryResolve(proposal, status, meeting)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This proposal has already been resolved");
        }
    }

    private boolean tryResolve(MeetingTimeChangeProposalEntity proposal, ProposalStatus status, MeetingEntity meeting) {
        OffsetDateTime now = OffsetDateTime.now();
        if (proposalRepository.transitionStatus(proposal.getId(), ProposalStatus.PENDING, status, now) == 0) {
            return false;
        }
        proposal.setStatus(status);
        proposal.setResolvedAt(now);

        String type = switch (status) {
            case APPROVED -> "MEETING_TIME_CHANGE_APPROVED";
            case REJECTED -> "MEETING_TIME_CHANGE_REJECTED";
            case EXPIRED -> "MEETING_TIME_CHANGE_EXPIRED";
            case CANCELLED -> "MEETING_TIME_CHANGE_CANCELLED";
            case PENDING -> "MEETING_TIME_CHANGE_PENDING";
        };
        for (UserEntity recipient : recipientsOf(meeting).values()) {
            Map<String, String> perUser = new LinkedHashMap<>(
                    resultVars(meeting, proposal, MeetingTimes.zoneFor(recipient.getTimezone(), defaultTimezone)));
            perUser.put("name", displayName(recipient));
            notificationService.push(recipient.getId(), type, CATEGORY, type, perUser);
        }
        return true;
    }

    /** Pending proposals the user still has to vote on, oldest first. */
    @Transactional(readOnly = true)
    public List<TimeChangeProposalResponse> listAwaitingVote(long userId) {
        return proposalRepository.findAwaitingVote(userId, PageRequest.of(0, AWAITING_VOTE_LIMIT)).stream()
                .map(proposal -> toResponse(proposal, userId))
                .toList();
    }

    /** Proposals the user made (PROPOSER) or was asked to vote on (VOTER), newest first. */
    @Transactional(readOnly = true)
    public CursorPage<TimeChangeProposalResponse> list(long userId, ProposalRole role, ProposalStatus status,
                                                       String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        OffsetDateTime cursorTime = decoded == null ? null : decoded.time();
        Long cursorId = decoded == null ? null : decoded.id();
        PageRequest page = PageRequest.of(0, limit + 1);
        List<MeetingTimeChangeProposalEntity> rows = role == ProposalRole.PROPOSER
                ? proposalRepository.listProposedBy(userId, status, cursorTime, cursorId, page)
                : proposalRepository.listVotedOnBy(userId, status, cursorTime, cursorId, page);

        boolean hasMore = rows.size() > limit;
        List<MeetingTimeChangeProposalEntity> items = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore && !items.isEmpty()
                ? CursorCodec.encode(items.getLast().getTimeSent(), items.getLast().getId())
                : null;
        return new CursorPage<>(items.stream().map(proposal -> toResponse(proposal, userId)).toList(), nextCursor);
    }

    private TimeChangeProposalResponse toResponse(MeetingTimeChangeProposalEntity proposal, long viewerId) {
        MeetingEntity meeting = proposal.getMeeting();
        VoteStatus myVote = voteRepository.findByProposalIdAndUserId(proposal.getId(), viewerId)
                .map(MeetingTimeChangeVoteEntity::getStatus)
                .orElse(null);
        List<ScheduleConflict> conflicts = conflictService.scheduleConflictsFor(
                viewerId, proposal.getProposedStartTime(), proposal.getProposedEndTime(), meeting.getId());
        return new TimeChangeProposalResponse(
                proposal.getId(),
                meeting.getId(),
                meeting.getTitle(),
                meeting.getCreator() != null && meeting.getCreator().getId().equals(viewerId),
                proposal.getProposedBy() == null ? null : proposal.getProposedBy().getId(),
                proposal.getProposedBy() == null ? null : proposal.getProposedBy().getUsername(),
                proposal.getProposedStartTime(),
                proposal.getProposedEndTime(),
                proposal.getStatus(),
                proposal.getTimeSent(),
                proposal.getResolvedAt(),
                myVote,
                conflicts);
    }

    private PendingTimeChange toPendingSummary(MeetingTimeChangeProposalEntity proposal) {
        return new PendingTimeChange(
                proposal.getId(),
                proposal.getProposedBy() == null ? null : proposal.getProposedBy().getId(),
                proposal.getProposedStartTime(),
                proposal.getProposedEndTime());
    }

    // Times are formatted in the recipient's profile time zone, so vars are built per recipient.
    private Map<String, String> requestVars(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal,
                                            ZoneId zone) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", meeting.getTitle());
        vars.put("old_start", MeetingTimes.format(meeting.getStartTime(), meeting.getEndTime(), zone));
        vars.put("new_start", MeetingTimes.format(proposal.getProposedStartTime(), proposal.getProposedEndTime(), zone));
        vars.put("link", appBaseUrl + "/meetings/" + meeting.getId());
        return vars;
    }

    private Map<String, String> resultVars(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal,
                                           ZoneId zone) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", meeting.getTitle());
        boolean approved = proposal.getStatus() == ProposalStatus.APPROVED;
        vars.put("start_time", MeetingTimes.format(
                approved ? proposal.getProposedStartTime() : meeting.getStartTime(),
                approved ? proposal.getProposedEndTime() : meeting.getEndTime(),
                zone));
        vars.put("link", appBaseUrl + "/meetings/" + meeting.getId());
        return vars;
    }

    private void notifyParticipants(MeetingEntity meeting, long actorUserId, String type,
                                    Function<ZoneId, Map<String, String>> varsForZone) {
        for (UserEntity recipient : recipientsOf(meeting).values()) {
            if (recipient.getId().equals(actorUserId)) {
                continue;
            }
            Map<String, String> perUser = new LinkedHashMap<>(varsForZone.apply(
                    MeetingTimes.zoneFor(recipient.getTimezone(), defaultTimezone)));
            perUser.put("name", displayName(recipient));
            notificationService.push(recipient.getId(), type, CATEGORY, type, perUser);
        }
    }

    private static Map<Long, UserEntity> recipientsOf(MeetingEntity meeting) {
        Map<Long, UserEntity> recipients = new LinkedHashMap<>();
        for (MeetingParticipantEntity p : meeting.getParticipants()) {
            recipients.putIfAbsent(p.getUser().getId(), p.getUser());
        }
        if (meeting.getCreator() != null) {
            recipients.putIfAbsent(meeting.getCreator().getId(), meeting.getCreator());
        }
        return recipients;
    }

    private static String displayName(UserEntity user) {
        return user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : user.getUsername();
    }
}

