package com.meetingplanner.domains.group.controller;

import com.meetingplanner.common.api.dto.*;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.group.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public CursorPage<PostResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                         @RequestParam(required = false) String cursor,
                                         @RequestParam(defaultValue = "20") int size) {
        return postService.list(CurrentUser.from(jwt).id(), groupId, cursor, size);
    }

    @GetMapping("/{postId}/comments")
    public CursorPage<PostCommentResponse> listComments(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                                        @PathVariable long postId,
                                                        @RequestParam(required = false) String cursor,
                                                        @RequestParam(defaultValue = "20") int size) {
        return postService.listComments(CurrentUser.from(jwt).id(), groupId, postId, cursor, size);
    }

    @PostMapping
    public PostResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                               @Valid @RequestBody PostCreateRequest request) {
        return postService.create(CurrentUser.from(jwt).id(), groupId, request);
    }

    @GetMapping("/{postId}")
    public PostResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                            @PathVariable long postId) {
        return postService.get(CurrentUser.from(jwt).id(), groupId, postId);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                       @PathVariable long postId) {
        postService.delete(CurrentUser.from(jwt).id(), postId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{postId}/comments")
    public ResponseEntity<Void> setCommentsEnabled(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                                   @PathVariable long postId,
                                                   @Valid @RequestBody CommentsEnabledRequest request) {
        postService.setCommentsEnabled(CurrentUser.from(jwt).id(), postId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments")
    public PostCommentResponse addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                          @PathVariable long postId,
                                          @Valid @RequestBody PostCommentCreateRequest request) {
        return postService.addComment(CurrentUser.from(jwt).id(), postId, request);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal Jwt jwt, @PathVariable long groupId,
                                              @PathVariable long postId, @PathVariable long commentId) {
        postService.deleteComment(CurrentUser.from(jwt).id(), commentId);
        return ResponseEntity.noContent().build();
    }
}