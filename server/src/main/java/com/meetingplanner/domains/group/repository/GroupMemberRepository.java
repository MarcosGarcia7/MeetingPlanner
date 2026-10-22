package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

    List<GroupMemberEntity> findByGroupId(Long groupId);

    List<GroupMemberEntity> findByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    Optional<GroupMemberEntity> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("select m.group.id from GroupMemberEntity m where m.user.id = :userId")
    Set<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}
