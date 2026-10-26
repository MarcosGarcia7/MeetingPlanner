package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupBanEntity;
import com.meetingplanner.domains.group.model.GroupBanId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBanRepository extends JpaRepository<GroupBanEntity, GroupBanId> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupBanEntity> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("""
            select b from GroupBanEntity b
            where b.group.id = :groupId
              and (cast(:cursorTime as OffsetDateTime) is null
                   or b.bannedAt < :cursorTime
                   or (b.bannedAt = :cursorTime and b.user.id < :cursorUserId))
            order by b.bannedAt desc, b.user.id desc
            """)
    List<GroupBanEntity> listByGroup(@Param("groupId") Long groupId,
                                     @Param("cursorTime") OffsetDateTime cursorTime,
                                     @Param("cursorUserId") Long cursorUserId,
                                     Pageable pageable);
}
