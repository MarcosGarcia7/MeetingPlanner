package com.meetingplanner.domains.meeting.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.InviteCheckResponse;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.request.service.JoinRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final JoinRequestService joinRequestService;

    public MeetingController(MeetingService meetingService, JoinRequestService joinRequestService) {
        this.meetingService = meetingService;
        this.joinRequestService = joinRequestService;
    }

    @PostMapping
    public MeetingResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MeetingCreateRequest request) {
        return meetingService.create(CurrentUser.from(jwt).id(), request);
    }

    @GetMapping
    public CursorPage<MeetingSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(required = false) Long groupId,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(defaultValue = "20") int size) {
        return meetingService.listMine(CurrentUser.from(jwt).id(), groupId, cursor, size);
    }

    @GetMapping("/created")
    public CursorPage<MeetingSummaryResponse> listCreated(@AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam(required = false) String cursor,
                                                          @RequestParam(defaultValue = "20") int size) {
        return meetingService.listCreated(CurrentUser.from(jwt).id(), cursor, size);
    }

    @GetMapping("/upcoming")
    public List<MeetingSummaryResponse> listUpcoming(@AuthenticationPrincipal Jwt jwt) {
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

    @DeleteMapping("/{meetingId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                                  @PathVariable long userId) {
        meetingService.removeParticipant(CurrentUser.from(jwt).id(), meetingId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{meetingId}/participants/me")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId) {
        meetingService.leaveMeeting(CurrentUser.from(jwt).id(), meetingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{meetingId}/invites")
    public InviteBatchResponse invite(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                      @Valid @RequestBody MeetingInviteBatchRequest request) {
        return meetingService.inviteBatch(CurrentUser.from(jwt).id(), meetingId, request);
    }

    @PostMapping("/{meetingId}/invites/check")
    public InviteCheckResponse checkInvites(@AuthenticationPrincipal Jwt jwt, @PathVariable long meetingId,
                                            @Valid @RequestBody MeetingInviteBatchRequest request) {
        return meetingService.checkInvites(CurrentUser.from(jwt).id(), meetingId, request);
    }

    @PostMapping("/{meetingId}/join-requests")
    public ResponseEntity<JoinRequestResponse> requestJoin(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable long meetingId) {
        JoinRequestResponse response = joinRequestService.requestMeetingJoin(CurrentUser.from(jwt).id(), meetingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{meetingId}/join-requests")
    public CursorPage<JoinRequestResponse> listJoinRequests(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable long meetingId,
                                                            @RequestParam(required = false) String cursor,
                                                            @RequestParam(defaultValue = "20") int size) {
        return joinRequestService.listForMeeting(CurrentUser.from(jwt).id(), meetingId, cursor, size);
    }
}