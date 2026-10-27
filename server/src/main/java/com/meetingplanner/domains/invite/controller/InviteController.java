package com.meetingplanner.domains.invite.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.invite.service.InviteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping
    public CursorPage<InviteResponse> listMine(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) InviteStatus status,
                                               @RequestParam(required = false) TargetKind kind,
                                               @RequestParam(required = false) String cursor,
                                               @RequestParam(defaultValue = "20") int size) {
        return inviteService.listMine(CurrentUser.from(jwt).id(), status, kind, cursor, size);
    }

    @PostMapping("/group/{inviteId}/accept")
    public InviteResponse acceptGroupInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable long inviteId) {
        return inviteService.acceptGroupInvite(CurrentUser.from(jwt).id(), inviteId);
    }

    @PostMapping("/group/{inviteId}/decline")
    public InviteResponse declineGroupInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable long inviteId) {
        return inviteService.declineGroupInvite(CurrentUser.from(jwt).id(), inviteId);
    }

    @PostMapping("/meeting/{inviteId}/accept")
    public InviteResponse acceptMeetingInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable long inviteId) {
        return inviteService.acceptMeetingInvite(CurrentUser.from(jwt).id(), inviteId);
    }

    @PostMapping("/meeting/{inviteId}/decline")
    public InviteResponse declineMeetingInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable long inviteId) {
        return inviteService.declineMeetingInvite(CurrentUser.from(jwt).id(), inviteId);
    }
}