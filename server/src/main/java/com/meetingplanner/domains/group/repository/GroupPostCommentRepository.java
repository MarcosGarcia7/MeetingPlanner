package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupPostCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupPostCommentRepository extends JpaRepository<GroupPostCommentEntity, Long> {

    List<GroupPostCommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    void deleteByPostId(Long postId);
}