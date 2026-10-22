package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserAvailabilityResponse;
import com.meetingplanner.common.api.dto.UserAvailabilityUpdateRequest;
import com.meetingplanner.common.api.exceptions.AvailabilityNotSetException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.user.model.UserAvailabilityEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;


@Service
public class UserAvailabilityService {

    private final UserAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final UserProfileAuthorizationService authorization;

    public UserAvailabilityService(UserAvailabilityRepository availabilityRepository,
                                    UserRepository userRepository,
                                    UserProfileAuthorizationService authorization) {
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public UserAvailabilityResponse getAvailability(long userId) {
        UserAvailabilityEntity av = availabilityRepository.findById(userId)
                        .orElseThrow(AvailabilityNotSetException::new);

        return new UserAvailabilityResponse(
                    userId,
                    av.getSunAvailability(),
                    av.getMonAvailability(),
                    av.getTueAvailability(),
                    av.getWedAvailability(),
                    av.getThuAvailability(),
                    av.getFriAvailability(),
                    av.getSatAvailability(),
                    av.isAvailabilitySet()
            );
    }

    @Transactional(readOnly = true)
    public UserAvailabilityResponse getAvailability(long viewerId, String username) {
        Long userId = userRepository.findIdByUsername(username);

        if (userId == null) throw new ResourceNotFoundException("User not found");

        if (!authorization.canViewSchedule(viewerId, userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        UserAvailabilityEntity av = availabilityRepository.findById(userId)
                .orElseThrow(AvailabilityNotSetException::new);

        if (!av.isAvailabilitySet()) throw new AvailabilityNotSetException();

        return new UserAvailabilityResponse(
                userId,
                av.getSunAvailability(),
                av.getMonAvailability(),
                av.getTueAvailability(),
                av.getWedAvailability(),
                av.getThuAvailability(),
                av.getFriAvailability(),
                av.getSatAvailability(),
                av.isAvailabilitySet()
        );
    }

    @Transactional
    public UserAvailabilityResponse updateAvailability(long userId, UserAvailabilityUpdateRequest request) {
        UserAvailabilityEntity av = availabilityRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found"));

        if (request.sunday() != null) av.setSunAvailability(toPrimitive(request.sunday()));
        if (request.monday() != null) av.setMonAvailability(toPrimitive(request.monday()));
        if (request.tuesday() != null) av.setTueAvailability(toPrimitive(request.tuesday()));
        if (request.wednesday() != null) av.setWedAvailability(toPrimitive(request.wednesday()));
        if (request.thursday() != null) av.setThuAvailability(toPrimitive(request.thursday()));
        if (request.friday() != null) av.setFriAvailability(toPrimitive(request.friday()));
        if (request.saturday() != null) av.setSatAvailability(toPrimitive(request.saturday()));
        av.setAvailabilitySet(request.availabilitySet());

        availabilityRepository.save(av);

        return new UserAvailabilityResponse(
                userId,
                av.getSunAvailability(),
                av.getMonAvailability(),
                av.getTueAvailability(),
                av.getWedAvailability(),
                av.getThuAvailability(),
                av.getFriAvailability(),
                av.getSatAvailability(),
                av.isAvailabilitySet()
        );
    }

    private int[][] toPrimitive(Integer[][] source) {
        if (source == null) return new int[0][];

        return Arrays.stream(source)
                .map(pair -> {
                    if (pair == null) return new int[2];

                    int[] primitivePair = new int[2];

                    primitivePair[0] = (pair.length > 0 && pair[0] != null) ? pair[0] : 0;
                    primitivePair[1] = (pair.length > 1 && pair[1] != null) ? pair[1] : 0;

                    return primitivePair;
                })
                .toArray(int[][]::new);
    }
}
