package com.meetingplanner.domains.search.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.GroupSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.UserSearchResult;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.group.service.GroupService;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final UserService userService;
    private final GroupService groupService;
    private final MeetingService meetingService;

    public SearchController(UserService userService, GroupService groupService, MeetingService meetingService) {
        this.userService = userService;
        this.groupService = groupService;
        this.meetingService = meetingService;
    }

    @GetMapping("/users")
    public CursorPage<UserSearchResult> users(@RequestParam String q,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(defaultValue = "20") int size) {
        return userService.search(q, cursor, size);
    }

    @GetMapping("/groups")
    public CursorPage<GroupSummaryResponse> groups(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam String q,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(defaultValue = "20") int size) {
        return groupService.search(CurrentUser.from(jwt).id(), q, cursor, size);
    }

    @GetMapping("/meetings")
    public CursorPage<MeetingSummaryResponse> meetings(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestParam String q,
                                                       @RequestParam(required = false) String cursor,
                                                       @RequestParam(defaultValue = "20") int size) {
        return meetingService.search(CurrentUser.from(jwt).id(), q, cursor, size);
    }
}
