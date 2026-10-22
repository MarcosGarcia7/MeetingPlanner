package com.meetingplanner.domains.group.controller;

import com.meetingplanner.common.api.dto.*;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.group.service.GroupService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final JoinRequestService joinRequestService;

    public GroupController(GroupService groupService, JoinRequestService joinRequestService) {
        this.groupService = groupService;
        this.joinRequestService = joinRequestService;
    }

    @PostMapping
    public GroupResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GroupCreateRequest request) {
        return groupService.create(CurrentUser.from(jwt).id(), request);
    }

    @GetMapping
    public CursorPage<GroupSummaryResponse> listMine(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(defaultValue = "20") int size) {
        return groupService.listMine(CurrentUser.from(jwt).id(), cursor, size);
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        return groupService.get(CurrentUser.from(jwt).id(), groupId);
    }

    @PatchMapping("/{groupId}")
    public GroupResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                @Valid @RequestBody GroupUpdateRequest request) {
        return groupService.update(CurrentUser.from(jwt).id(), groupId, request);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        groupService.delete(CurrentUser.from(jwt).id(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Void> join(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        groupService.join(CurrentUser.from(jwt).id(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/join-requests")
    public ResponseEntity<JoinRequestResponse> requestJoin(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable long groupId) {
        JoinRequestResponse response = joinRequestService.requestGroupJoin(CurrentUser.from(jwt).id(), groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}/join-requests")
    public CursorPage<JoinRequestResponse> listJoinRequests(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable long groupId,
                                                            @RequestParam(required = false) String cursor,
                                                            @RequestParam(defaultValue = "20") int size) {
        return joinRequestService.listForGroup(CurrentUser.from(jwt).id(), groupId, cursor, size);
    }

    @PostMapping("/{groupId}/invites")
    public InviteBatchResponse invite(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                      @Valid @RequestBody GroupInviteBatchRequest request) {
        return groupService.inviteBatch(CurrentUser.from(jwt).id(), groupId, request);
    }

    @GetMapping("/{groupId}/invites")
    public CursorPage<InviteResponse> listInvites(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                                  @RequestParam(required = false) String cursor,
                                                  @RequestParam(defaultValue = "20") int size) {
        return groupService.listInvites(CurrentUser.from(jwt).id(), groupId, cursor, size);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                             @PathVariable long userId) {
        groupService.removeMember(CurrentUser.from(jwt).id(), groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> promoteOrDemoteMember(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                              @PathVariable long userId) {
        groupService.promoteOrDemoteMember(CurrentUser.from(jwt).id(), groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupId}/owner/{userId}")
    public ResponseEntity<Void> transferOwnership(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                                  @PathVariable long userId) {
        groupService.transferOwnership(CurrentUser.from(jwt).id(), groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        groupService.leave(CurrentUser.from(jwt).id(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/privacy")
    public ResponseEntity<Void> setShareSchedule(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                                 @Valid @RequestBody GroupPrivacyRequest request) {
        groupService.setShareSchedule(CurrentUser.from(jwt).id(), groupId, request.shareSchedule());
        return ResponseEntity.noContent().build();
    }
}