package com.meetingplanner.domains.request.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.request.service.JoinRequestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @GetMapping
    public CursorPage<JoinRequestResponse> listMine(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(defaultValue = "20") int size) {
        return joinRequestService.listMine(CurrentUser.from(jwt).id(), cursor, size);
    }

    @PostMapping("/group/{requestId}/approve")
    public JoinRequestResponse approveGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.approveGroup(CurrentUser.from(jwt).id(), requestId);
    }

    @PostMapping("/group/{requestId}/decline")
    public JoinRequestResponse declineGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.declineGroup(CurrentUser.from(jwt).id(), requestId);
    }

    @PostMapping("/group/{requestId}/cancel")
    public JoinRequestResponse cancelGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.cancelGroup(CurrentUser.from(jwt).id(), requestId);
    }

    @PostMapping("/meeting/{requestId}/approve")
    public JoinRequestResponse approveMeeting(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.approveMeeting(CurrentUser.from(jwt).id(), requestId);
    }

    @PostMapping("/meeting/{requestId}/decline")
    public JoinRequestResponse declineMeeting(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.declineMeeting(CurrentUser.from(jwt).id(), requestId);
    }

    @PostMapping("/meeting/{requestId}/cancel")
    public JoinRequestResponse cancelMeeting(@AuthenticationPrincipal Jwt jwt, @PathVariable long requestId) {
        return joinRequestService.cancelMeeting(CurrentUser.from(jwt).id(), requestId);
    }
}
