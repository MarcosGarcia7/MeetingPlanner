package com.meetingplanner.domains.group.controller;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.PostResponse;
import com.meetingplanner.common.security.CurrentUser;
import com.meetingplanner.domains.group.service.PostService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostFeedController {

    private final PostService postService;

    public PostFeedController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/recent")
    public CursorPage<PostResponse> listRecent(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) String cursor,
                                               @RequestParam(defaultValue = "20") int size) {
        return postService.listRecent(CurrentUser.from(jwt).id(), cursor, size);
    }
}
