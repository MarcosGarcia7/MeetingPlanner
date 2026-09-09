package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileAuthorizationService authorization;

    public UserServiceImpl(UserRepository userRepository, UserProfileAuthorizationService authorization) {
        this.userRepository = userRepository;
        this.authorization = authorization;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMe(long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public UserProfileResponse updateMe(long userId, UserProfileUpdateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(long viewerId, String username) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void deleteMe(long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}