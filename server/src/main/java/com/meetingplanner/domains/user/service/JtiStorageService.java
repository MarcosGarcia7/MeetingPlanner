package com.meetingplanner.domains.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JtiStorageService {

    private final StringRedisTemplate redisTemplate;

    public JtiStorageService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${REFRESH_TOKEN_EXPIRATION_MS}")
    private long refreshTokenExpiryMs;

    public void addValidJti(String userId, String jti) {
        String key = "user:jti:" + userId;
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, refreshTokenExpiryMs / 60000, TimeUnit.MINUTES);
    }

    public boolean isValidJti(String userId, String jti) {
        String key = "user:jti:" + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, jti));
    }

    public boolean consumeJti(String userId, String jti) {
        String key = "user:jti:" + userId;
        Long removed = redisTemplate.opsForSet().remove(key, jti);
        return removed != null && removed > 0;
    }

    public void revokeAllJtis(String userId) {
        String key = "user:jti:" + userId;
        redisTemplate.delete(key);
    }

    public void invalidateJti(String userId, String jti) {
        String key = "user:jti:" + userId;
        redisTemplate.opsForSet().remove(key, jti);
    }
}
