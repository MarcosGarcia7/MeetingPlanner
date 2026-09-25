package com.meetingplanner.domains.friend.repository;

import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipId;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<FriendshipEntity, FriendshipId> {

    Optional<FriendshipEntity> findByUserIdAndFriendId(Long userId, Long friendId);

    boolean existsByUserIdAndFriendIdAndStatus(Long userId, Long friendId, FriendshipStatus status);

    List<FriendshipEntity> findByUserIdAndStatus(Long userId, FriendshipStatus status);

    List<FriendshipEntity> findByFriendIdAndStatus(Long userId, FriendshipStatus status);

    @Query("select f from FriendshipEntity f where f.status = 'ACCEPTED' and (f.user.id = :userId or f.friend.id = :userId)")
    List<FriendshipEntity> findAcceptedByUserId(@Param("userId") Long userId);

    @Query("select f from FriendshipEntity f where f.status = 'BLOCKED' and (f.user.id = :userId or f.friend.id = :userId)")
    List<FriendshipEntity> findBlockedByUserId(@Param("userId") Long userId);

    @Query("select case when count(f) > 0 then true else false end " +
            "from FriendshipEntity f where f.status = 'ACCEPTED' " +
            "and ((f.user.id = :a and f.friend.id = :b) or (f.user.id = :b and f.friend.id = :a))")
    boolean areFriends(@Param("a") Long a, @Param("b") Long b);
}