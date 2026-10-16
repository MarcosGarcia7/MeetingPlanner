package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPostEntity, Long> {

    @Query("""
            select p from GroupPostEntity p
            where p.group.id = :groupId
              and (:cursorTime is null
                   or p.createdAt < :cursorTime
                   or (p.createdAt = :cursorTime and p.id < :cursorId))
            order by p.createdAt desc, p.id desc
            """)
    List<GroupPostEntity> listByGroup(@Param("groupId") Long groupId,
                                      @Param("cursorTime") OffsetDateTime cursorTime,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);
}