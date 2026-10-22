package com.meetingplanner.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Missing or invalid access token\"}";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (authException instanceof InvalidBearerTokenException) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"api\", error=\"invalid_token\"");
        } else {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"api\"");
        }
        response.getWriter().write(BODY);
    }
}
