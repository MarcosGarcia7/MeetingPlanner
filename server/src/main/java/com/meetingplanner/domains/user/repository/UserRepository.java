package com.meetingplanner.domains.user.repository;

import com.meetingplanner.domains.user.model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    Long findIdByUsername(String username);

    // Hides the viewer, unverified accounts, and anyone in a BLOCKED pair with the viewer (either direction).
    @Query("""
            select u from UserEntity u
            where u.id <> :viewerId
              and u.emailVerifiedAt is not null
              and not exists (
                   select 1 from FriendshipEntity f
                   where f.status = 'BLOCKED'
                     and ((f.user.id = :viewerId and f.friend.id = u.id)
                          or (f.user.id = u.id and f.friend.id = :viewerId)))
              and (lower(u.username) like :pattern escape '!'
                   or lower(coalesce(u.firstName, '')) like :pattern escape '!'
                   or lower(coalesce(u.lastName, '')) like :pattern escape '!'
                   or lower(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))) like :pattern escape '!')
              and (:cursorValue is null
                   or lower(u.username) > :cursorValue
                   or (lower(u.username) = :cursorValue and u.id > :cursorId))
            order by lower(u.username) asc, u.id asc
            """)
    List<UserEntity> search(@Param("viewerId") Long viewerId,
                            @Param("pattern") String pattern,
                            @Param("cursorValue") String cursorValue,
                            @Param("cursorId") Long cursorId,
                            Pageable pageable);
}
