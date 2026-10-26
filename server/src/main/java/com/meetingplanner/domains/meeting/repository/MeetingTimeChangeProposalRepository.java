package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeProposalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingTimeChangeProposalRepository extends JpaRepository<MeetingTimeChangeProposalEntity, Long> {

    Optional<MeetingTimeChangeProposalEntity> findByMeetingIdAndStatus(Long meetingId, ProposalStatus status);

    Optional<MeetingTimeChangeProposalEntity> findByIdAndMeetingId(Long id, Long meetingId);

    List<MeetingTimeChangeProposalEntity> findByStatusAndTimeSentBefore(ProposalStatus status, OffsetDateTime cutoff);

    @Query("""
            select p from MeetingTimeChangeProposalEntity p
            join fetch p.meeting
            left join fetch p.proposedBy
            where p.status = com.meetingplanner.common.api.model.ProposalStatus.PENDING
              and exists (select 1 from MeetingTimeChangeVoteEntity v
                          where v.proposal = p
                            and v.user.id = :userId
                            and v.status = com.meetingplanner.common.api.model.VoteStatus.PENDING)
            order by p.timeSent asc, p.id asc
            """)
    List<MeetingTimeChangeProposalEntity> findAwaitingVote(@Param("userId") Long userId, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingTimeChangeProposalEntity p
            set p.status = :to, p.resolvedAt = :at
            where p.id = :id and p.status = :from
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("from") ProposalStatus from,
                         @Param("to") ProposalStatus to,
                         @Param("at") OffsetDateTime at);
}
