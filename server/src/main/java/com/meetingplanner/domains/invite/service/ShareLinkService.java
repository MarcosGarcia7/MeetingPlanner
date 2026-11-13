package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.CreateGroupShareLinkRequest;
import com.meetingplanner.common.api.dto.CreateMeetingShareLinkRequest;
import com.meetingplanner.common.api.dto.ShareLinkJoinResponse;
import com.meetingplanner.common.api.dto.ShareLinkPreviewResponse;
import com.meetingplanner.common.api.dto.ShareLinkResponse;

import java.util.Optional;

public interface ShareLinkService {

    Optional<ShareLinkResponse> getGroupLink(long userId, long groupId);

    ShareLinkResponse createGroupLink(long userId, long groupId, CreateGroupShareLinkRequest request);

    void revokeGroupLink(long userId, long groupId);

    Optional<ShareLinkResponse> getMeetingLink(long userId, long meetingId);

    ShareLinkResponse createMeetingLink(long userId, long meetingId, CreateMeetingShareLinkRequest request);

    void revokeMeetingLink(long userId, long meetingId);

    /** viewerId is null for anonymous viewers. */
    ShareLinkPreviewResponse preview(Long viewerId, String token);

    ShareLinkJoinResponse join(long userId, String token);

    /** Meeting links only. Sends the guest a regular meeting invite email; answers the same whatever happens. */
    void requestGuestInvite(String token, String email, String clientIp);
}
