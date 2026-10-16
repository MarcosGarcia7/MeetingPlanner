package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;

public interface UserService {

    UserProfileResponse getMe(long userId);

    UserProfileResponse updateMe(long userId, UserProfileUpdateRequest request);

    UserProfileResponse getByUsername(long viewerId, String username);

    long resolveUserId(String username);

    void deleteMe(long userId);
}