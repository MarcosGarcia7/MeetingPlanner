package com.meetingplanner.domains.group.controller;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupPrivacyRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.GroupUpdateRequest;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.security.CurrentUser;
import com.meetingplanner.domains.group.service.GroupService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public GroupResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GroupCreateRequest request) {
        return groupService.create(CurrentUser.from(jwt).id(), request);
    }

    @GetMapping
    public List<GroupResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return groupService.listMine(CurrentUser.from(jwt).id());
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

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<Void> invite(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                       @Valid @RequestBody MemberUsernameRequest request) {
        groupService.invite(CurrentUser.from(jwt).id(), groupId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{groupId}/invites")
    public List<InviteResponse> listInvites(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId) {
        return groupService.listInvites(CurrentUser.from(jwt).id(), groupId);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                             @PathVariable long userId) {
        groupService.removeMember(CurrentUser.from(jwt).id(), groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> promoteMember(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                              @PathVariable long userId) {
        groupService.promoteMember(CurrentUser.from(jwt).id(), groupId, userId);
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