package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    List<GroupEntity> findByOwnerId(Long ownerId);

    boolean existsByGroupName(String groupName);

    Optional<GroupEntity> findByOwnerIdAndId(Long ownerId, Long id);
}
