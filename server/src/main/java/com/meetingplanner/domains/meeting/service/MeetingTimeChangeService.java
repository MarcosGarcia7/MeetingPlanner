package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.PendingTimeChange;
import com.meetingplanner.common.api.dto.TimeChangeConflict;
import com.meetingplanner.common.api.dto.TimeChangeProposalResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        proposalRepository.save(proposal);

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

        notifyParticipants(meeting, actorUserId, "MEETING_TIME_CHANGE_REQUEST", requestVars(meeting, proposal));
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

        vote.setStatus(approve ? VoteStatus.APPROVED : VoteStatus.DECLINED);
        vote.setRespondedAt(OffsetDateTime.now());
        voteRepository.save(vote);

        MeetingEntity meeting = proposal.getMeeting();
        if (!approve) {
            resolve(proposal, ProposalStatus.REJECTED, meeting);
        } else if (voteRepository.countByProposalIdAndStatus(proposalId, VoteStatus.PENDING) == 0) {
            applyTimes(meeting, proposal);
            resolve(proposal, ProposalStatus.APPROVED, meeting);
        }
        return toResponse(proposal, userId);
    }

    @Transactional
    public TimeChangeProposalResponse cancel(long userId, long meetingId, long proposalId) {
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
        for (MeetingTimeChangeProposalEntity proposal : stale) {
            resolve(proposal, ProposalStatus.EXPIRED, proposal.getMeeting());
        }
        return stale.size();
    }

    private void applyTimes(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal) {
        meeting.setStartTime(proposal.getProposedStartTime());
        meeting.setEndTime(proposal.getProposedEndTime());
        meetingRepository.save(meeting);
    }

    private void resolve(MeetingTimeChangeProposalEntity proposal, ProposalStatus status, MeetingEntity meeting) {
        proposal.setStatus(status);
        proposal.setResolvedAt(OffsetDateTime.now());
        proposalRepository.save(proposal);

        String type = switch (status) {
            case APPROVED -> "MEETING_TIME_CHANGE_APPROVED";
            case REJECTED -> "MEETING_TIME_CHANGE_REJECTED";
            case EXPIRED -> "MEETING_TIME_CHANGE_EXPIRED";
            case CANCELLED -> "MEETING_TIME_CHANGE_CANCELLED";
            case PENDING -> "MEETING_TIME_CHANGE_PENDING";
        };
        Map<String, String> vars = resultVars(meeting, proposal);
        for (UserEntity recipient : recipientsOf(meeting).values()) {
            Map<String, String> perUser = new LinkedHashMap<>(vars);
            perUser.put("name", displayName(recipient));
            notificationService.push(recipient.getId(), type, CATEGORY, type, perUser);
        }
    }

    private TimeChangeProposalResponse toResponse(MeetingTimeChangeProposalEntity proposal, long viewerId) {
        MeetingEntity meeting = proposal.getMeeting();
        VoteStatus myVote = voteRepository.findByProposalIdAndUserId(proposal.getId(), viewerId)
                .map(MeetingTimeChangeVoteEntity::getStatus)
                .orElse(null);
        List<TimeChangeConflict> conflicts = conflictService.findScheduleConflicts(
                        viewerId, proposal.getProposedStartTime(), proposal.getProposedEndTime(), meeting.getId())
                .stream()
                .map(m -> new TimeChangeConflict(m.getId(), m.getTitle(), m.getStartTime()))
                .toList();
        return new TimeChangeProposalResponse(
                proposal.getId(),
                meeting.getId(),
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

    private Map<String, String> requestVars(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", meeting.getTitle());
        vars.put("old_start", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(meeting.getStartTime()));
        vars.put("new_start", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(proposal.getProposedStartTime()));
        vars.put("link", appBaseUrl + "/meetings/" + meeting.getId());
        return vars;
    }

    private Map<String, String> resultVars(MeetingEntity meeting, MeetingTimeChangeProposalEntity proposal) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", meeting.getTitle());
        vars.put("start_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                proposal.getStatus() == ProposalStatus.APPROVED
                        ? proposal.getProposedStartTime()
                        : meeting.getStartTime()));
        vars.put("link", appBaseUrl + "/meetings/" + meeting.getId());
        return vars;
    }

    private void notifyParticipants(MeetingEntity meeting, long actorUserId, String type, Map<String, String> vars) {
        for (UserEntity recipient : recipientsOf(meeting).values()) {
            if (recipient.getId().equals(actorUserId)) {
                continue;
            }
            Map<String, String> perUser = new LinkedHashMap<>(vars);
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

