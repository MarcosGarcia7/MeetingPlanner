package com.meetingplanner.domains.meeting.controller;

import com.meetingplanner.common.api.dto.TimeChangeProposalResponse;
import com.meetingplanner.common.api.dto.TimeChangeVoteRequest;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingTimeChangeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings/{meetingId}/time-change")
public class MeetingTimeChangeController {

    private final MeetingTimeChangeService timeChangeService;

    public MeetingTimeChangeController(MeetingTimeChangeService timeChangeService) {
        this.timeChangeService = timeChangeService;
    }

    @GetMapping
    public TimeChangeProposalResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId) {
        return timeChangeService.getProposal(CurrentUser.from(jwt).id(), meetingId);
    }

    @PostMapping("/{proposalId}/vote")
    public TimeChangeProposalResponse vote(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                           @PathVariable long proposalId,
                                           @Valid @RequestBody TimeChangeVoteRequest request) {
        return timeChangeService.vote(CurrentUser.from(jwt).id(), meetingId, proposalId,
                Boolean.TRUE.equals(request.approve()));
    }

    @PostMapping("/{proposalId}/cancel")
    public TimeChangeProposalResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                             @PathVariable long proposalId) {
        return timeChangeService.cancel(CurrentUser.from(jwt).id(), meetingId, proposalId);
    }
}
