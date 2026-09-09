package com.meetingplanner.common.api.security;

import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(long id, String username) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.parseLong(jwt.getSubject()), jwt.getClaim("username"));
    }
}