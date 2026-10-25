package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierLimitsTest {

    // 64-char local part + 189-char domain = 254 characters, a structurally valid address.
    private static final String EMAIL_254 =
            "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
    private static final String EMAIL_255 =
            "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62);

    @Test
    void memberUsernameUsesUsernameLimit() {
        assertTrue(violatedPaths(new MemberUsernameRequest("a".repeat(20))).isEmpty());
        assertEquals(Set.of("username"), violatedPaths(new MemberUsernameRequest("a".repeat(21))));
    }

    @Test
    void meetingInviteEmailUsesEmailLimit() {
        assertTrue(violatedPaths(new MeetingInviteRequest(null, EMAIL_254)).isEmpty());
        assertEquals(Set.of("email"), violatedPaths(new MeetingInviteRequest(null, EMAIL_255)));
    }

    @Test
    void verifyEmailTokenIsBounded() {
        assertTrue(violatedPaths(new VerifyEmailRequest("a".repeat(128))).isEmpty());
        assertEquals(Set.of("token"), violatedPaths(new VerifyEmailRequest("a".repeat(129))));
    }

    @Test
    void refreshTokenIsBounded() {
        assertTrue(violatedPaths(new RefreshRequest("a".repeat(4096))).isEmpty());
        assertEquals(Set.of("refreshToken"), violatedPaths(new RefreshRequest("a".repeat(4097))));
    }

    @Test
    void logoutRefreshTokenIsBounded() {
        assertTrue(violatedPaths(new LogoutRequest("a".repeat(4096), false)).isEmpty());
        assertEquals(Set.of("refreshToken"), violatedPaths(new LogoutRequest("a".repeat(4097), false)));
    }
}
