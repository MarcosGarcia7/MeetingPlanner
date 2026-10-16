package com.meetingplanner.domains.friend.repository;

import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipId;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<FriendshipEntity, FriendshipId> {

    Optional<FriendshipEntity> findByUserIdAndFriendId(Long userId, Long friendId);

    boolean existsByUserIdAndFriendIdAndStatus(Long userId, Long friendId, FriendshipStatus status);

    @Query("""
            select f from FriendshipEntity f
            where f.status = :status
              and f.user.id = :userId
              and (:cursorTime is null
                   or f.createdAt < :cursorTime
                   or (f.createdAt = :cursorTime and f.friend.id < :cursorId))
            order by f.createdAt desc, f.friend.id desc
            """)
    List<FriendshipEntity> listByStatusAsUser(@Param("userId") Long userId,
                                              @Param("status") FriendshipStatus status,
                                              @Param("cursorTime") OffsetDateTime cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    @Query("""
            select f from FriendshipEntity f
            where f.status = :status
              and f.friend.id = :userId
              and (:cursorTime is null
                   or f.createdAt < :cursorTime
                   or (f.createdAt = :cursorTime and f.user.id < :cursorId))
            order by f.createdAt desc, f.user.id desc
            """)
    List<FriendshipEntity> listByStatusAsFriend(@Param("userId") Long userId,
                                                @Param("status") FriendshipStatus status,
                                                @Param("cursorTime") OffsetDateTime cursorTime,
                                                @Param("cursorId") Long cursorId,
                                                Pageable pageable);

    @Query("""
            select f from FriendshipEntity f
            where f.status = :status
              and f.user.id = :userId
              and ((:incoming = true and f.requester.id <> :userId)
                   or (:incoming = false and f.requester.id = :userId))
              and (:cursorTime is null
                   or f.createdAt < :cursorTime
                   or (f.createdAt = :cursorTime and f.friend.id < :cursorId))
            order by f.createdAt desc, f.friend.id desc
            """)
    List<FriendshipEntity> listRequestsAsUser(@Param("userId") Long userId,
                                              @Param("status") FriendshipStatus status,
                                              @Param("incoming") boolean incoming,
                                              @Param("cursorTime") OffsetDateTime cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    @Query("""
            select f from FriendshipEntity f
            where f.status = :status
              and f.friend.id = :userId
              and ((:incoming = true and f.requester.id <> :userId)
                   or (:incoming = false and f.requester.id = :userId))
              and (:cursorTime is null
                   or f.createdAt < :cursorTime
                   or (f.createdAt = :cursorTime and f.user.id < :cursorId))
            order by f.createdAt desc, f.user.id desc
            """)
    List<FriendshipEntity> listRequestsAsFriend(@Param("userId") Long userId,
                                                @Param("status") FriendshipStatus status,
                                                @Param("incoming") boolean incoming,
                                                @Param("cursorTime") OffsetDateTime cursorTime,
                                                @Param("cursorId") Long cursorId,
                                                Pageable pageable);

    @Query("select case when count(f) > 0 then true else false end " +
            "from FriendshipEntity f where f.status = 'ACCEPTED' " +
            "and ((f.user.id = :a and f.friend.id = :b) or (f.user.id = :b and f.friend.id = :a))")
    boolean areFriends(@Param("a") Long a, @Param("b") Long b);
}