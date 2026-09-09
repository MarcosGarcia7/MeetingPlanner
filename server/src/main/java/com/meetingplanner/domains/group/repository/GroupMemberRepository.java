package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

    List<GroupMemberEntity> findByGroupId(Long groupId);

    List<GroupMemberEntity> findByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMemberEntity> findByGroupIdAndUserId(Long groupId, Long userId);
}
