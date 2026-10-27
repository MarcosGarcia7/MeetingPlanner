package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {

    List<GroupMemberEntity> findByGroupId(Long groupId);

    List<GroupMemberEntity> findByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    Optional<GroupMemberEntity> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("select m.group.id from GroupMemberEntity m where m.user.id = :userId")
    Set<Long> findGroupIdsByUserId(@Param("userId") Long userId);

    interface GroupCount {
        Long getGroupId();

        long getCount();
    }

    @Query("""
            select m.group.id as groupId, count(m) as count from GroupMemberEntity m
            where m.group.id in :groupIds
            group by m.group.id
            """)
    List<GroupCount> countByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    // Members of each group who are accepted friends of the viewer (friendships are one row per pair).
    @Query("""
            select m.group.id as groupId, count(m) as count
            from GroupMemberEntity m, FriendshipEntity f
            where m.group.id in :groupIds
              and f.status = 'ACCEPTED'
              and ((f.user.id = :viewerId and f.friend.id = m.user.id)
                   or (f.friend.id = :viewerId and f.user.id = m.user.id))
            group by m.group.id
            """)
    List<GroupCount> countFriendMembersByGroupIds(@Param("viewerId") Long viewerId,
                                                  @Param("groupIds") Collection<Long> groupIds);
}
