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
              and (cast(:cursorTime as OffsetDateTime) is null
                   or p.createdAt < :cursorTime
                   or (p.createdAt = :cursorTime and p.id < :cursorId))
            order by p.createdAt desc, p.id desc
            """)
    List<GroupPostEntity> listByGroup(@Param("groupId") Long groupId,
                                      @Param("cursorTime") OffsetDateTime cursorTime,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);

    // Posts from every group the user belongs to, minus authors blocked in either direction.
    @Query("""
            select p from GroupPostEntity p
            join fetch p.author a
            join fetch p.group g
            where exists (select 1 from GroupMemberEntity m
                          where m.group = g and m.user.id = :userId)
              and not exists (
                  select 1 from FriendshipEntity f
                  where f.status = 'BLOCKED'
                    and ((f.user.id = :userId and f.friend.id = a.id)
                         or (f.friend.id = :userId and f.user.id = a.id)))
              and (cast(:cursorTime as OffsetDateTime) is null
                   or p.createdAt < :cursorTime
                   or (p.createdAt = :cursorTime and p.id < :cursorId))
            order by p.createdAt desc, p.id desc
            """)
    List<GroupPostEntity> listRecentForMember(@Param("userId") Long userId,
                                              @Param("cursorTime") OffsetDateTime cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);
}