package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.common.api.model.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingInviteRepository extends JpaRepository<MeetingInviteEntity, Long> {

    List<MeetingInviteEntity> findByRecipientId(Long recipientId);

    List<MeetingInviteEntity> findByRecipientIdAndStatus(Long recipientId, InviteStatus status);

    List<MeetingInviteEntity> findByRecipientEmail(String recipientEmail);

    List<MeetingInviteEntity> findBySenderId(Long senderId);

    boolean existsByMeetingIdAndRecipientId(Long meetingId, Long recipientId);

    Optional<MeetingInviteEntity> findByTokenHash(String tokenHash);

    Optional<MeetingInviteEntity> findByMeetingIdAndRecipientEmail(Long meetingId, String recipientEmail);
}
