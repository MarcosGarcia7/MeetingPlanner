package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.common.api.model.VoteStatus;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingTimeChangeVoteRepository extends JpaRepository<MeetingTimeChangeVoteEntity, Long> {

    List<MeetingTimeChangeVoteEntity> findByProposalId(Long proposalId);

    Optional<MeetingTimeChangeVoteEntity> findByProposalIdAndUserId(Long proposalId, Long userId);

    long countByProposalIdAndStatus(Long proposalId, VoteStatus status);

    boolean existsByProposalIdAndStatus(Long proposalId, VoteStatus status);
}
