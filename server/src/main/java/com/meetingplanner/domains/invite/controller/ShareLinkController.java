package com.meetingplanner.domains.invite.controller;

import com.meetingplanner.common.api.dto.CreateGroupShareLinkRequest;
import com.meetingplanner.common.api.dto.CreateMeetingShareLinkRequest;
import com.meetingplanner.common.api.dto.ShareLinkGuestRequest;
import com.meetingplanner.common.api.dto.ShareLinkJoinResponse;
import com.meetingplanner.common.api.dto.ShareLinkPreviewResponse;
import com.meetingplanner.common.api.dto.ShareLinkResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.invite.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Shareable join links. Managing a link needs a JWT; previewing a link and the meeting guest form are public
// (see SecurityConfig), and joining needs a JWT.
@RestController
@RequestMapping("/api")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    public ShareLinkController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @GetMapping("/groups/{groupId}/share-link")
    public ResponseEntity<ShareLinkResponse> getGroupLink(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        return shareLinkService.getGroupLink(CurrentUser.from(jwt).id(), groupId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/groups/{groupId}/share-link")
    public ResponseEntity<ShareLinkResponse> createGroupLink(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable long groupId,
                                                             @Valid @RequestBody CreateGroupShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shareLinkService.createGroupLink(CurrentUser.from(jwt).id(), groupId, request));
    }

    @DeleteMapping("/groups/{groupId}/share-link")
    public ResponseEntity<Void> revokeGroupLink(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        shareLinkService.revokeGroupLink(CurrentUser.from(jwt).id(), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/meetings/{meetingId}/share-link")
    public ResponseEntity<ShareLinkResponse> getMeetingLink(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable long meetingId) {
        return shareLinkService.getMeetingLink(CurrentUser.from(jwt).id(), meetingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/meetings/{meetingId}/share-link")
    public ResponseEntity<ShareLinkResponse> createMeetingLink(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable long meetingId,
                                                               @Valid @RequestBody CreateMeetingShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shareLinkService.createMeetingLink(CurrentUser.from(jwt).id(), meetingId, request));
    }

    @DeleteMapping("/meetings/{meetingId}/share-link")
    public ResponseEntity<Void> revokeMeetingLink(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId) {
        shareLinkService.revokeMeetingLink(CurrentUser.from(jwt).id(), meetingId);
        return ResponseEntity.noContent().build();
    }

    // Public. A signed-in viewer (bearer present) also learns whether they're already a member.
    @GetMapping("/share-links/{token}")
    public ShareLinkPreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return shareLinkService.preview(jwt == null ? null : CurrentUser.from(jwt).id(), token);
    }

    @PostMapping("/share-links/{token}/join")
    public ShareLinkJoinResponse join(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return shareLinkService.join(CurrentUser.from(jwt).id(), token);
    }

    // Public. Always 202 so the response doesn't reveal whether the address has an account or an invite.
    @PostMapping("/share-links/{token}/guest")
    public ResponseEntity<Void> guest(@PathVariable String token,
                                      @Valid @RequestBody ShareLinkGuestRequest request,
                                      HttpServletRequest httpRequest) {
        shareLinkService.requestGuestInvite(token, request.email(), resolveClientIp(httpRequest));
        return ResponseEntity.accepted().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
