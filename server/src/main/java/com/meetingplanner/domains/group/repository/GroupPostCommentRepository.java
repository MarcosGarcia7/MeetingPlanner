package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupPostCommentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface GroupPostCommentRepository extends JpaRepository<GroupPostCommentEntity, Long> {

    @Query("""
            select c from GroupPostCommentEntity c
            where c.post.id = :postId
              and (cast(:cursorTime as OffsetDateTime) is null
                   or c.createdAt > :cursorTime
                   or (c.createdAt = :cursorTime and c.id > :cursorId))
            order by c.createdAt asc, c.id asc
            """)
    List<GroupPostCommentEntity> listByPost(@Param("postId") Long postId,
                                            @Param("cursorTime") OffsetDateTime cursorTime,
                                            @Param("cursorId") Long cursorId,
                                            Pageable pageable);

    void deleteByPostId(Long postId);
}