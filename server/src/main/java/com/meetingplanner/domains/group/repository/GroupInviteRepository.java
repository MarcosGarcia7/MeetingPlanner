package com.meetingplanner.domains.group.repository;

import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInviteEntity, Long> {

    List<GroupInviteEntity> findByRecipientId(Long recipientId);

    List<GroupInviteEntity> findByRecipientIdAndStatus(Long recipientId, InviteStatus status);

    List<GroupInviteEntity> findByRecipientEmail(String recipientEmail);

    List<GroupInviteEntity> findBySenderId(Long senderId);

    boolean existsByGroupIdAndRecipientId(Long groupId, Long recipientId);

    Optional<GroupInviteEntity> findByTokenHash(String tokenHash);

    Optional<GroupInviteEntity> findByGroupIdAndRecipientEmail(Long groupId, String recipientEmail);
}
