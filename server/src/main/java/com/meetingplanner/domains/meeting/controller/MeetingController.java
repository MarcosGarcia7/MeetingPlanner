package com.meetingplanner.domains.meeting.controller;

import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;
import com.meetingplanner.common.api.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public MeetingResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MeetingCreateRequest request) {
        return meetingService.create(CurrentUser.from(jwt).id(), request);
    }

    @GetMapping
    public List<MeetingResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long groupId) {
        long userId = CurrentUser.from(jwt).id();
        if (groupId != null) {
            return meetingService.listForGroup(userId, groupId);
        }
        return List.of();
    }

    @GetMapping("/upcoming")
    public List<MeetingResponse> listUpcoming(@AuthenticationPrincipal Jwt jwt) {
        return meetingService.listUpcoming(CurrentUser.from(jwt).id());
    }

    @GetMapping("/{meetingId}")
    public MeetingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId) {
        return meetingService.get(CurrentUser.from(jwt).id(), meetingId);
    }

    @PatchMapping("/{meetingId}")
    public MeetingResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                  @Valid @RequestBody MeetingUpdateRequest request) {
        return meetingService.update(CurrentUser.from(jwt).id(), meetingId, request);
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId) {
        meetingService.delete(CurrentUser.from(jwt).id(), meetingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{meetingId}/participants")
    public ParticipantResponse addParticipant(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                              @Valid @RequestBody MemberUsernameRequest request) {
        return meetingService.addParticipant(CurrentUser.from(jwt).id(), meetingId, request);
    }

    @DeleteMapping("/{meetingId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                                  @PathVariable long userId) {
        meetingService.removeParticipant(CurrentUser.from(jwt).id(), meetingId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{meetingId}/invites")
    public InviteResponse invite(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                 @Valid @RequestBody MemberUsernameRequest request) {
        return meetingService.invite(CurrentUser.from(jwt).id(), meetingId, request);
    }
}