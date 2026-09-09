package com.meetingplanner.domains.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "lockout:counter:";
    private static final long DEFAULT_WINDOW_SECONDS = 900;

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long incrementAttempts(String targetKey) {
        return incrementAttempts(targetKey, DEFAULT_WINDOW_SECONDS);
    }

    public long incrementAttempts(String targetKey, long windowSeconds) {
        String key = KEY_PREFIX + targetKey;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return attempts != null ? attempts : 0;
    }

    public long getAttemptCount(String targetKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + targetKey);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    public boolean isBlocked(String targetKey, int maxAttempts) {
        return getAttemptCount(targetKey) >= maxAttempts;
    }

    public void resetAttempts(String targetKey) {
        redisTemplate.delete(KEY_PREFIX + targetKey);
    }

    public long getRemainingWindowSeconds(String targetKey) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + targetKey, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }
}