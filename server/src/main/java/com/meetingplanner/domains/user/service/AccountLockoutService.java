package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.model.LockoutBucket;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class AccountLockoutService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCKOUT_KEY_PREFIX = "account:lockout:";

    public AccountLockoutService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAccountLocked(String username) {
        String key = LOCKOUT_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void lockAccount(String userId, int currentTier, String reason, long durationMinutes) {
        String key = LOCKOUT_KEY_PREFIX + userId;
        LockoutBucket bucket = new LockoutBucket(currentTier, Instant.now(), reason);
        redisTemplate.opsForValue().set(key, bucket, durationMinutes, TimeUnit.MINUTES);
    }

    public LockoutBucket getLockoutDetails(String userId) {
        String key = LOCKOUT_KEY_PREFIX + userId;
        Object data = redisTemplate.opsForValue().get(key);
        return data != null ? (LockoutBucket) data : null;
    }

    public long getRemainingLockoutTimeInSeconds(String username) {
        String key = LOCKOUT_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }

    public void unlockAccount(String userId) {
        redisTemplate.delete(LOCKOUT_KEY_PREFIX + userId);
    }
}
