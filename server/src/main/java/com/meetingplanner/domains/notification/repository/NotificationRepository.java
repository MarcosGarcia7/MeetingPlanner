package com.meetingplanner.domains.notification.repository;

import com.meetingplanner.domains.notification.model.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
            select n from NotificationEntity n
            where n.user.id = :userId
              and (cast(:cursorTime as OffsetDateTime) is null
                   or n.createdAt < :cursorTime
                   or (n.createdAt = :cursorTime and n.id < :cursorId))
            order by n.createdAt desc, n.id desc
            """)
    List<NotificationEntity> listForUser(@Param("userId") Long userId,
                                         @Param("cursorTime") OffsetDateTime cursorTime,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);

    @Transactional
    @Modifying
    @Query("""
            delete from NotificationEntity n
            where (n.readAt is not null and n.createdAt < :readBefore)
               or n.createdAt < :createdBefore
            """)
    int deleteOld(@Param("readBefore") OffsetDateTime readBefore,
                  @Param("createdBefore") OffsetDateTime createdBefore);
}
