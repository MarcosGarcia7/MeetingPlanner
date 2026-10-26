package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.api.dto.UserSearchResult;

public interface UserService {

    UserProfileResponse getMe(long userId);

    UserProfileResponse updateMe(long userId, UserProfileUpdateRequest request);

    UserProfileResponse getByUsername(long viewerId, String username);

    CursorPage<UserSearchResult> search(long userId, String query, String cursor, int size);

    long resolveUserId(String username);

    void deleteMe(long userId);
}