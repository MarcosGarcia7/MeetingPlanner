package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.TimeChangeProposalResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.common.api.model.VoteStatus;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeProposalEntity;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeVoteEntity;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.repository.MeetingTimeChangeProposalRepository;
import com.meetingplanner.domains.meeting.repository.MeetingTimeChangeVoteRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTimeChangeServiceTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingTimeChangeProposalRepository proposalRepository;
    @Mock
    private MeetingTimeChangeVoteRepository voteRepository;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MeetingAuthorizationService authorization;

    @InjectMocks
    private MeetingTimeChangeService service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static MeetingEntity meeting(long id, UserEntity creator, UserEntity... participants) {
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(id);
        meeting.setCreator(creator);
        meeting.setTitle("Test meeting");
        meeting.setStartTime(OffsetDateTime.parse("2026-10-20T10:00:00Z"));
        for (UserEntity participant : participants) {
            meeting.addParticipant(participant);
        }
        return meeting;
    }

    private static MeetingTimeChangeProposalEntity proposal(long id, MeetingEntity meeting, UserEntity proposer,
                                                            OffsetDateTime start) {
        MeetingTimeChangeProposalEntity proposal = new MeetingTimeChangeProposalEntity();
        proposal.setId(id);
        proposal.setMeeting(meeting);
        proposal.setProposedBy(proposer);
        proposal.setProposedStartTime(start);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setTimeSent(OffsetDateTime.now().minusHours(1));
        return proposal;
    }

    private static MeetingTimeChangeVoteEntity vote(MeetingTimeChangeProposalEntity proposal, UserEntity voter) {
        MeetingTimeChangeVoteEntity vote = new MeetingTimeChangeVoteEntity();
        vote.setProposal(proposal);
        vote.setUser(voter);
        vote.setStatus(VoteStatus.PENDING);
        return vote;
    }

    @Test
    void createProposalNotifiesOtherParticipantsAndRecordsTheirVotes() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        OffsetDateTime newStart = OffsetDateTime.parse("2026-10-21T10:00:00Z");

        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(proposalRepository.findByMeetingIdAndStatus(10L, ProposalStatus.PENDING)).thenReturn(Optional.empty());
        when(proposalRepository.saveAndFlush(any(MeetingTimeChangeProposalEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(voteRepository.save(any(MeetingTimeChangeVoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conflictService.findScheduleConflicts(eq(1L), any(), any(), eq(10L))).thenReturn(List.of());

        TimeChangeProposalResponse response = service.createProposal(1L, meeting, newStart, null);

        assertNotNull(response);
        verify(voteRepository).save(any(MeetingTimeChangeVoteEntity.class));
        verify(notificationService).push(eq(2L), eq("MEETING_TIME_CHANGE_REQUEST"), eq("meeting_updates"),
                eq("MEETING_TIME_CHANGE_REQUEST"), anyMap());
    }

    @Test
    void approvingLastVoteAppliesTimesAndResolvesProposal() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        OffsetDateTime newStart = OffsetDateTime.parse("2026-10-21T10:00:00Z");
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice, newStart);
        MeetingTimeChangeVoteEntity bobVote = vote(proposal, bob);

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));
        when(voteRepository.findByProposalIdAndUserId(5L, 2L)).thenReturn(Optional.of(bobVote));
        when(voteRepository.countByProposalIdAndStatus(5L, VoteStatus.PENDING)).thenReturn(0L);
        when(meetingRepository.save(meeting)).thenReturn(meeting);
        when(proposalRepository.transitionStatus(eq(5L), eq(ProposalStatus.PENDING), eq(ProposalStatus.APPROVED),
                any(OffsetDateTime.class))).thenReturn(1);
        when(conflictService.findScheduleConflicts(eq(2L), any(), any(), eq(10L))).thenReturn(List.of());

        service.vote(2L, 10L, 5L, true);

        assertEquals(ProposalStatus.APPROVED, proposal.getStatus());
        assertEquals(newStart, meeting.getStartTime());
        verify(notificationService).push(eq(1L), eq("MEETING_TIME_CHANGE_APPROVED"), eq("meeting_updates"),
                eq("MEETING_TIME_CHANGE_APPROVED"), anyMap());
    }

    @Test
    void anyDeclineResolvesProposalAsRejected() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        OffsetDateTime newStart = OffsetDateTime.parse("2026-10-21T10:00:00Z");
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice, newStart);
        MeetingTimeChangeVoteEntity bobVote = vote(proposal, bob);

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));
        when(voteRepository.findByProposalIdAndUserId(5L, 2L)).thenReturn(Optional.of(bobVote));
        when(proposalRepository.transitionStatus(eq(5L), eq(ProposalStatus.PENDING), eq(ProposalStatus.REJECTED),
                any(OffsetDateTime.class))).thenReturn(1);
        when(conflictService.findScheduleConflicts(eq(2L), any(), any(), eq(10L))).thenReturn(List.of());

        service.vote(2L, 10L, 5L, false);

        assertEquals(ProposalStatus.REJECTED, proposal.getStatus());
        assertEquals(OffsetDateTime.parse("2026-10-20T10:00:00Z"), meeting.getStartTime());
        verify(meetingRepository, never()).save(any(MeetingEntity.class));
    }

    @Test
    void proposerCanCancelPendingProposal() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.transitionStatus(eq(5L), eq(ProposalStatus.PENDING), eq(ProposalStatus.CANCELLED),
                any(OffsetDateTime.class))).thenReturn(1);
        when(conflictService.findScheduleConflicts(eq(1L), any(), any(), eq(10L))).thenReturn(List.of());

        service.cancel(1L, 10L, 5L);

        assertEquals(ProposalStatus.CANCELLED, proposal.getStatus());
    }

    @Test
    void nonProposerCannotCancelProposal() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));

        assertThrows(ResourceNotFoundException.class, () -> service.cancel(2L, 10L, 5L));
    }

    @Test
    void expireStaleResolvesOverdueProposals() {
        ReflectionTestUtils.setField(service, "deadlineHours", 48L);
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));
        proposal.setTimeSent(OffsetDateTime.now().minusHours(72));

        when(proposalRepository.findByStatusAndTimeSentBefore(eq(ProposalStatus.PENDING), any()))
                .thenReturn(List.of(proposal));
        when(proposalRepository.transitionStatus(eq(5L), eq(ProposalStatus.PENDING), eq(ProposalStatus.EXPIRED),
                any(OffsetDateTime.class))).thenReturn(1);

        int expired = service.expireStale();

        assertEquals(1, expired);
        assertEquals(ProposalStatus.EXPIRED, proposal.getStatus());
    }

    @Test
    void expireStaleSkipsProposalsResolvedConcurrently() {
        ReflectionTestUtils.setField(service, "deadlineHours", 48L);
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));
        proposal.setTimeSent(OffsetDateTime.now().minusHours(72));

        when(proposalRepository.findByStatusAndTimeSentBefore(eq(ProposalStatus.PENDING), any()))
                .thenReturn(List.of(proposal));
        when(proposalRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        int expired = service.expireStale();

        assertEquals(0, expired);
        assertEquals(ProposalStatus.PENDING, proposal.getStatus());
        verifyNoInteractions(notificationService);
    }

    @Test
    void voteLocksMeetingBeforeReadingProposal() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));
        when(voteRepository.findByProposalIdAndUserId(5L, 2L)).thenReturn(Optional.of(vote(proposal, bob)));
        when(proposalRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(1);

        service.vote(2L, 10L, 5L, false);

        InOrder order = inOrder(meetingRepository, proposalRepository);
        order.verify(meetingRepository).findByIdForUpdate(10L);
        order.verify(proposalRepository).findByIdAndMeetingId(5L, 10L);
    }

    @Test
    void voteOnMissingMeetingIsNotFound() {
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.vote(2L, 10L, 5L, true));

        verifyNoInteractions(proposalRepository, voteRepository);
    }

    @Test
    void finalApprovalLosingRaceIsConflict() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(5L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(proposalRepository.findByIdAndMeetingId(5L, 10L)).thenReturn(Optional.of(proposal));
        when(voteRepository.findByProposalIdAndUserId(5L, 2L)).thenReturn(Optional.of(vote(proposal, bob)));
        when(voteRepository.countByProposalIdAndStatus(5L, VoteStatus.PENDING)).thenReturn(0L);
        when(proposalRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.vote(2L, 10L, 5L, true));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(ProposalStatus.PENDING, proposal.getStatus());
        verifyNoInteractions(notificationService);
    }

    @Test
    void createProposalUniqueRaceIsMappedToConflict() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);

        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(proposalRepository.findByMeetingIdAndStatus(10L, ProposalStatus.PENDING)).thenReturn(Optional.empty());
        when(proposalRepository.saveAndFlush(any(MeetingTimeChangeProposalEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createProposal(1L, meeting, OffsetDateTime.parse("2026-10-21T10:00:00Z"), null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(voteRepository, notificationService);
    }

    @Test
    void listAwaitingVoteMapsPendingProposalsWithMeetingTitle() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);
        MeetingTimeChangeProposalEntity proposal = proposal(20L, meeting, alice,
                OffsetDateTime.parse("2026-10-21T10:00:00Z"));
        when(proposalRepository.findAwaitingVote(eq(2L), any())).thenReturn(List.of(proposal));
        when(voteRepository.findByProposalIdAndUserId(20L, 2L)).thenReturn(Optional.of(vote(proposal, bob)));

        List<TimeChangeProposalResponse> result = service.listAwaitingVote(2L);

        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().id());
        assertEquals("Test meeting", result.getFirst().meetingTitle());
        assertEquals("alice", result.getFirst().proposedByUsername());
        assertEquals(VoteStatus.PENDING, result.getFirst().myVote());
    }

    @Test
    void listAwaitingVoteIsEmptyWhenNothingPending() {
        when(proposalRepository.findAwaitingVote(eq(2L), any())).thenReturn(List.of());

        assertEquals(List.of(), service.listAwaitingVote(2L));
        verifyNoInteractions(conflictService);
    }
}
