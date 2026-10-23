package com.meetingplanner.domains.request.service;

import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.service.MeetingAuthorizationService;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private MeetingJoinRequestRepository meetingJoinRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAuthorizationService groupAuthorization;
    @Mock
    private MeetingAuthorizationService meetingAuthorization;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BlockService blockService;

    @InjectMocks
    private JoinRequestServiceImpl service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void blockedUserCannotRequestMeetingJoin() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setPrivate(false);
        group.setOwner(creator);
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(10L);
        meeting.setCreator(creator);
        meeting.setGroup(group);
        meeting.setPrivate(false);

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.isParticipantOrCreator(2L, meeting)).thenReturn(false);
        when(blockService.isBlocked(2L, 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestMeetingJoin(2L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(meetingJoinRequestRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void blockedUserCanStillRequestGroupJoin() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setGroupName("Team");
        group.setPrivate(true);
        group.setOwner(owner);

        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isOwner(2L, group)).thenReturn(false);
        when(groupAuthorization.isMember(2L, group)).thenReturn(false);
        when(groupJoinRequestRepository.existsByGroupIdAndRequesterIdAndStatus(
                eq(5L), eq(2L), any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(groupMemberRepository.findByGroupId(5L)).thenReturn(List.of());

        service.requestGroupJoin(2L, 5L);

        verify(groupJoinRequestRepository).save(any());
        verify(blockService, never()).isBlocked(anyLong(), anyLong());
    }
}
