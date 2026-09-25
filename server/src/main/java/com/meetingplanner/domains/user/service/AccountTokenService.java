package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.domains.user.model.AccountTokenEntity;
import com.meetingplanner.domains.user.model.AccountTokenType;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.AccountTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AccountTokenService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final AccountTokenRepository accountTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountTokenService(AccountTokenRepository accountTokenRepository) {
        this.accountTokenRepository = accountTokenRepository;
    }

    @Transactional
    public String create(UserEntity user, AccountTokenType type, String email, String ip, Duration ttl) {
        accountTokenRepository.deleteByUserIdAndTokenType(user.getId(), type);

        String raw = UUID.randomUUID().toString().replace("-", "") + hex(secureRandom, 16);
        AccountTokenEntity token = new AccountTokenEntity();
        token.setUser(user);
        token.setTokenType(type);
        token.setTokenHash(sha256Hex(raw));
        token.setEmail(email);
        token.setRequestedIp(ip);
        token.setExpiresAt(OffsetDateTime.now().plus(ttl != null ? ttl : DEFAULT_TTL));
        accountTokenRepository.save(token);
        return raw;
    }

    @Transactional
    public AccountTokenEntity consume(String rawToken, AccountTokenType... acceptableTypes) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Token is required");
        }

        AccountTokenEntity token = accountTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        boolean acceptable = false;
        for (AccountTokenType type : acceptableTypes) {
            if (token.getTokenType() == type) {
                acceptable = true;
                break;
            }
        }
        if (!acceptable) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        if (token.getConsumedAt() != null) {
            throw new InvalidTokenException("Token has already been used");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Token has expired");
        }

        token.setConsumedAt(OffsetDateTime.now());
        accountTokenRepository.save(token);
        return token;
    }

    @Transactional
    public void deleteForUser(Long userId, AccountTokenType type) {
        accountTokenRepository.deleteByUserIdAndTokenType(userId, type);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String hex(SecureRandom random, int byteCount) {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}