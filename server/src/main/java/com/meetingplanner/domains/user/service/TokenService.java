package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.model.TokenPair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder accessJwtEncoder;
    private final JwtEncoder refreshJwtEncoder;

    @Value("${ACCESS_TOKEN_EXPIRATION_MS}")
    private long accessTokenExpiryMs;

    @Value("${REFRESH_TOKEN_EXPIRATION_MS}")
    private long refreshTokenExpiryMs;

    @Value("${JWT_ISSUER}")
    private String issuer;

    public TokenService(@Qualifier("accessJwtEncoder") JwtEncoder accessJwtEncoder,
                        @Qualifier("refreshJwtEncoder") JwtEncoder refreshJwtEncoder) {
        this.accessJwtEncoder = accessJwtEncoder;
        this.refreshJwtEncoder = refreshJwtEncoder;
    }

    public TokenPair generateTokenPair(String userId, String username) {
        Instant now = Instant.now();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessTokenExpiryMs))
                .subject(userId)
                .claim("username", username)
                .claim("type", "access")
                .build();
        String accessToken = this.accessJwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();

        String refreshJti = UUID.randomUUID().toString().replace("-", "");
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusMillis(refreshTokenExpiryMs))
                .subject(userId)
                .claim("type", "refresh")
                .claim("jti", refreshJti)
                .build();
        String refreshToken = this.refreshJwtEncoder.encode(JwtEncoderParameters.from(refreshClaims)).getTokenValue();

        return new TokenPair(accessToken, refreshToken, refreshJti);
    }
}
