package com.meetingplanner.domains.group.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/meetings")
public class GroupMeetingController {

    private final MeetingService meetingService;

    public GroupMeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public CursorPage<MeetingSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable long groupId,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(defaultValue = "20") int size) {
        return meetingService.listForGroup(CurrentUser.from(jwt).id(), groupId, cursor, size);
    }

    @PostMapping
    public MeetingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable long groupId,
                                  @Valid @RequestBody MeetingCreateRequest request) {
        return meetingService.createInGroup(CurrentUser.from(jwt).id(), groupId, request);
    }
}
