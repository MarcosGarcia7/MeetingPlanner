package com.meetingplanner.domains.user.repository;

import com.meetingplanner.domains.user.model.AccountTokenEntity;
import com.meetingplanner.domains.user.model.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, Long> {

    Optional<AccountTokenEntity> findByTokenHash(String tokenHash);

    int deleteByUserIdAndTokenType(Long userId, AccountTokenType tokenType);

    @Transactional
    @Modifying
    @Query("delete from AccountTokenEntity t where t.expiresAt < :now or t.consumedAt is not null")
    int deleteExpiredOrConsumed(@Param("now") OffsetDateTime now);
}
