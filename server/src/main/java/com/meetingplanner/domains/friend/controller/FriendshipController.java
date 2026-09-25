package com.meetingplanner.domains.friend.controller;

import com.meetingplanner.common.api.dto.FriendshipResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.security.CurrentUser;
import com.meetingplanner.domains.friend.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public List<FriendshipResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.listFriends(CurrentUser.from(jwt).id());
    }

    @GetMapping("/requests/incoming")
    public List<FriendshipResponse> incoming(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.listIncomingRequests(CurrentUser.from(jwt).id());
    }

    @GetMapping("/requests/outgoing")
    public List<FriendshipResponse> outgoing(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.listOutgoingRequests(CurrentUser.from(jwt).id());
    }

    @GetMapping("/blocked")
    public List<FriendshipResponse> blocked(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.listBlocked(CurrentUser.from(jwt).id());
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberUsernameRequest request) {
        friendshipService.sendRequest(CurrentUser.from(jwt).id(), request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/requests/{username}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        friendshipService.respond(CurrentUser.from(jwt).id(), username, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{username}/decline")
    public ResponseEntity<Void> decline(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        friendshipService.respond(CurrentUser.from(jwt).id(), username, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{username}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        friendshipService.cancel(CurrentUser.from(jwt).id(), username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/blocked")
    public ResponseEntity<Void> block(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberUsernameRequest request) {
        long targetId = friendshipService.resolveUserId(request.username());
        friendshipService.block(CurrentUser.from(jwt).id(), targetId);
        return ResponseEntity.noContent().build();
    }
}