package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GroupEntity g where g.id = :id")
    Optional<GroupEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select g from GroupEntity g
            where (g.owner.id = :userId
                   or exists (select 1 from GroupMemberEntity m
                              where m.group.id = g.id and m.user.id = :userId))
              and (:cursorId is null or g.id < :cursorId)
            order by g.id desc
            """)
    List<GroupEntity> listMine(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               Pageable pageable);

    List<GroupEntity> findByOwnerId(Long ownerId);

    boolean existsByGroupName(String groupName);

    boolean existsByGroupNameAndIdNot(String groupName, Long id);

    Optional<GroupEntity> findByOwnerIdAndId(Long ownerId, Long id);

    @Query("""
            select g from GroupEntity g
            where lower(g.groupName) like :pattern escape '!'
              and (:cursorValue is null
                   or lower(g.groupName) > :cursorValue
                   or (lower(g.groupName) = :cursorValue and g.id > :cursorId))
            order by lower(g.groupName) asc, g.id asc
            """)
    List<GroupEntity> search(@Param("pattern") String pattern,
                             @Param("cursorValue") String cursorValue,
                             @Param("cursorId") Long cursorId,
                             Pageable pageable);
}
