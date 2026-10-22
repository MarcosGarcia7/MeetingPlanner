package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserAvailabilityUpdateRequest;
import com.meetingplanner.common.api.exceptions.AvailabilityNotSetException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.user.model.UserAvailabilityEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAvailabilityServiceTest {

    @Mock
    private UserAvailabilityRepository availabilityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileAuthorizationService authorization;

    @InjectMocks
    private UserAvailabilityService service;

    private static UserAvailabilityEntity availability(boolean set) {
        UserAvailabilityEntity av = new UserAvailabilityEntity();
        av.setSunAvailability(new int[0][]);
        av.setMonAvailability(new int[0][]);
        av.setTueAvailability(new int[0][]);
        av.setWedAvailability(new int[0][]);
        av.setThuAvailability(new int[0][]);
        av.setFriAvailability(new int[0][]);
        av.setSatAvailability(new int[0][]);
        av.setAvailabilitySet(set);
        return av;
    }

    @Test
    void unknownUsernameIsNotFound() {
        when(userRepository.findIdByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.getAvailability(1L, "ghost"));

        verifyNoInteractions(availabilityRepository);
    }

    @Test
    void missingRowIsTreatedAsNotSetForSelf() {
        when(availabilityRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(AvailabilityNotSetException.class, () -> service.getAvailability(2L));
    }

    @Test
    void viewerSeesNotSetWhenRowMissing() {
        when(userRepository.findIdByUsername("bob")).thenReturn(2L);
        when(authorization.canViewSchedule(1L, 2L)).thenReturn(true);
        when(availabilityRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(AvailabilityNotSetException.class, () -> service.getAvailability(1L, "bob"));
    }

    @Test
    void updateWithMissingRowIsNotFound() {
        when(availabilityRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateAvailability(2L,
                new UserAvailabilityUpdateRequest(null, null, null, null, null, null, null, true)));

        verify(availabilityRepository, never()).save(any(UserAvailabilityEntity.class));
    }

    @Test
    void updatePersistsChangedFields() {
        UserAvailabilityEntity av = availability(false);
        when(availabilityRepository.findById(2L)).thenReturn(Optional.of(av));
        when(availabilityRepository.save(any(UserAvailabilityEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateAvailability(2L,
                new UserAvailabilityUpdateRequest(null, new Integer[][]{{9, 17}}, null, null, null, null, null, true));

        assertNotNull(response);
        assertTrue(av.isAvailabilitySet());
        assertEquals(1, av.getMonAvailability().length);
        assertEquals(9, av.getMonAvailability()[0][0]);
        assertEquals(17, av.getMonAvailability()[0][1]);
        verify(availabilityRepository).save(av);
    }
}
