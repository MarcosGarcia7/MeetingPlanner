package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPostEntity, Long> {

    List<GroupPostEntity> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
}