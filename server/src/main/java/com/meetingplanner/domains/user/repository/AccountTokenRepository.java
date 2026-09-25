package com.meetingplanner.domains.user.repository;

import com.meetingplanner.domains.user.model.AccountTokenEntity;
import com.meetingplanner.domains.user.model.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, Long> {

    Optional<AccountTokenEntity> findByTokenHash(String tokenHash);

    int deleteByUserIdAndTokenType(Long userId, AccountTokenType tokenType);
}