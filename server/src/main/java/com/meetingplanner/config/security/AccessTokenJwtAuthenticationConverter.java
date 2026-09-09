package com.meetingplanner.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;

public class AccessTokenJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        if (!"access".equals(jwt.getClaim("type"))) {
            throw new BadJwtException("The JWT is not an access token");
        }

        return new JwtAuthenticationToken(jwt, Collections.emptyList(), jwt.getSubject());
    }
}