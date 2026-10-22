package com.meetingplanner.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSecurityErrorHandlersTest {

    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Missing or invalid access token\"}";
    private static final String FORBIDDEN_BODY =
            "{\"error\":\"forbidden\",\"message\":\"Access is denied\"}";

    private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();
    private final JsonAccessDeniedHandler accessDeniedHandler = new JsonAccessDeniedHandler();

    @Test
    void missingTokenReturns401WithHouseBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(UNAUTHORIZED_BODY, response.getContentAsString());
        assertEquals("Bearer realm=\"api\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    void invalidTokenHintsInvalidTokenWithoutLeakingDetails() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InvalidBearerTokenException("The JWT is expired at 2026-01-01T00:00:00Z"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(UNAUTHORIZED_BODY, response.getContentAsString());
        assertEquals("Bearer realm=\"api\", error=\"invalid_token\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    void accessDeniedReturns403WithHouseBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("Denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(FORBIDDEN_BODY, response.getContentAsString());
        assertNull(response.getHeader("WWW-Authenticate"));
    }
}
