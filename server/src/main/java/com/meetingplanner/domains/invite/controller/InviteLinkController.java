package com.meetingplanner.domains.invite.controller;

import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.domains.invite.service.InviteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invite-links")
public class InviteLinkController {

    private final InviteService inviteService;

    public InviteLinkController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/group/{token}")
    public InviteResponse viewGroup(@PathVariable String token) {
        return inviteService.viewGroupInvite(token);
    }

    @GetMapping("/meeting/{token}")
    public InviteResponse viewMeeting(@PathVariable String token) {
        return inviteService.viewMeetingInvite(token);
    }

    @PostMapping("/group/{token}/accept")
    public InviteResponse acceptGroup(@PathVariable String token) {
        return inviteService.acceptGroupInviteByToken(token);
    }

    @PostMapping("/group/{token}/decline")
    public InviteResponse declineGroup(@PathVariable String token) {
        return inviteService.declineGroupInviteByToken(token);
    }

    @PostMapping("/meeting/{token}/accept")
    public InviteResponse acceptMeeting(@PathVariable String token) {
        return inviteService.acceptMeetingInviteByToken(token);
    }

    @PostMapping("/meeting/{token}/decline")
    public InviteResponse declineMeeting(@PathVariable String token) {
        return inviteService.declineMeetingInviteByToken(token);
    }

    @PostMapping("/meeting/{token}/leave")
    public InviteResponse leaveMeeting(@PathVariable String token) {
        return inviteService.leaveMeetingInviteByToken(token);
    }
}