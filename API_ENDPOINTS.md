# Server API Endpoints

96 endpoints across 14 controllers, generated from the Spring controllers in
`server/src/main/java/com/meetingplanner/domains/*/controller`.

## AuthController (9)

| Method | Path | Handler |
| --- | --- | --- |
| POST | `/api/auth/create` | `create` |
| POST | `/api/auth/login/email` | `loginEmail` |
| POST | `/api/auth/login/username` | `loginUsername` |
| POST | `/api/auth/refresh` | `refresh` |
| POST | `/api/auth/logout` | `logout` |
| POST | `/api/auth/verify-email` | `verifyEmail` |
| POST | `/api/auth/verify-email/resend` | `resendVerificationEmail` |
| POST | `/api/auth/forgot-password` | `forgotPassword` |
| POST | `/api/auth/reset-password` | `resetPassword` |

## FriendshipController (10)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/users/me/friends` | `list` |
| GET | `/api/users/me/friends/requests/incoming` | `incoming` |
| GET | `/api/users/me/friends/requests/outgoing` | `outgoing` |
| GET | `/api/users/me/friends/blocked` | `blocked` |
| POST | `/api/users/me/friends/requests` | `send` |
| POST | `/api/users/me/friends/requests/{username}/accept` | `accept` |
| POST | `/api/users/me/friends/requests/{username}/decline` | `decline` |
| DELETE | `/api/users/me/friends/requests/{username}` | `cancel` |
| POST | `/api/users/me/friends/blocked` | `block` |
| DELETE | `/api/users/me/friends/blocked/{username}` | `unblock` |

## GroupController (18)

| Method | Path | Handler |
| --- | --- | --- |
| POST | `/api/groups` | `create` |
| GET | `/api/groups` | `listMine` |
| GET | `/api/groups/{groupId}` | `get` |
| PATCH | `/api/groups/{groupId}` | `update` |
| DELETE | `/api/groups/{groupId}` | `delete` |
| POST | `/api/groups/{groupId}/join` | `join` |
| POST | `/api/groups/{groupId}/join-requests` | `requestJoin` |
| GET | `/api/groups/{groupId}/join-requests` | `listJoinRequests` |
| POST | `/api/groups/{groupId}/invites` | `invite` |
| GET | `/api/groups/{groupId}/invites` | `listInvites` |
| DELETE | `/api/groups/{groupId}/members/{userId}` | `removeMember` |
| POST | `/api/groups/{groupId}/bans/{userId}` | `banMember` |
| DELETE | `/api/groups/{groupId}/bans/{userId}` | `unbanMember` |
| GET | `/api/groups/{groupId}/bans` | `listBans` |
| PATCH | `/api/groups/{groupId}/members/{userId}` | `promoteOrDemoteMember` |
| PUT | `/api/groups/{groupId}/owner/{userId}` | `transferOwnership` |
| POST | `/api/groups/{groupId}/leave` | `leave` |
| PATCH | `/api/groups/{groupId}/privacy` | `setShareSchedule` |

## GroupMeetingController (2)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/groups/{groupId}/meetings` | `list` |
| POST | `/api/groups/{groupId}/meetings` | `create` |

## InviteController (5)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/invites` | `listMine` |
| POST | `/api/invites/group/{inviteId}/accept` | `acceptGroupInvite` |
| POST | `/api/invites/group/{inviteId}/decline` | `declineGroupInvite` |
| POST | `/api/invites/meeting/{inviteId}/accept` | `acceptMeetingInvite` |
| POST | `/api/invites/meeting/{inviteId}/decline` | `declineMeetingInvite` |

## InviteLinkController (7)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/invite-links/group/{token}` | `viewGroup` |
| GET | `/api/invite-links/meeting/{token}` | `viewMeeting` |
| POST | `/api/invite-links/group/{token}/accept` | `acceptGroup` |
| POST | `/api/invite-links/group/{token}/decline` | `declineGroup` |
| POST | `/api/invite-links/meeting/{token}/accept` | `acceptMeeting` |
| POST | `/api/invite-links/meeting/{token}/decline` | `declineMeeting` |
| POST | `/api/invite-links/meeting/{token}/leave` | `leaveMeeting` |

## JoinRequestController (7)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/requests` | `listMine` |
| POST | `/api/requests/group/{requestId}/approve` | `approveGroup` |
| POST | `/api/requests/group/{requestId}/decline` | `declineGroup` |
| POST | `/api/requests/group/{requestId}/cancel` | `cancelGroup` |
| POST | `/api/requests/meeting/{requestId}/approve` | `approveMeeting` |
| POST | `/api/requests/meeting/{requestId}/decline` | `declineMeeting` |
| POST | `/api/requests/meeting/{requestId}/cancel` | `cancelMeeting` |

## MeetingController (13)

| Method | Path | Handler |
| --- | --- | --- |
| POST | `/api/meetings` | `create` |
| GET | `/api/meetings` | `list` |
| GET | `/api/meetings/created` | `listCreated` |
| GET | `/api/meetings/upcoming` | `listUpcoming` |
| GET | `/api/meetings/{meetingId}` | `get` |
| PATCH | `/api/meetings/{meetingId}` | `update` |
| DELETE | `/api/meetings/{meetingId}` | `delete` |
| DELETE | `/api/meetings/{meetingId}/participants/{userId}` | `removeParticipant` |
| DELETE | `/api/meetings/{meetingId}/participants/me` | `leave` |
| POST | `/api/meetings/{meetingId}/invites` | `invite` |
| POST | `/api/meetings/{meetingId}/invites/check` | `checkInvites` |
| POST | `/api/meetings/{meetingId}/join-requests` | `requestJoin` |
| GET | `/api/meetings/{meetingId}/join-requests` | `listJoinRequests` |

## MeetingTimeChangeController (3)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/meetings/{meetingId}/time-change` | `get` |
| POST | `/api/meetings/{meetingId}/time-change/{proposalId}/vote` | `vote` |
| POST | `/api/meetings/{meetingId}/time-change/{proposalId}/cancel` | `cancel` |

## NotificationController (4)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/users/me/notifications` | `list` |
| GET | `/api/users/me/notifications/unread-count` | `unreadCount` |
| POST | `/api/users/me/notifications/{notificationId}/read` | `markRead` |
| POST | `/api/users/me/notifications/read-all` | `markAllRead` |

## NotificationPreferenceController (2)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/users/me/notification-preferences` | `get` |
| PUT | `/api/users/me/notification-preferences` | `update` |

## PostController (8)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/groups/{groupId}/posts` | `list` |
| GET | `/api/groups/{groupId}/posts/{postId}/comments` | `listComments` |
| POST | `/api/groups/{groupId}/posts` | `create` |
| GET | `/api/groups/{groupId}/posts/{postId}` | `get` |
| DELETE | `/api/groups/{groupId}/posts/{postId}` | `delete` |
| PATCH | `/api/groups/{groupId}/posts/{postId}/comments` | `setCommentsEnabled` |
| POST | `/api/groups/{groupId}/posts/{postId}/comments` | `addComment` |
| DELETE | `/api/groups/{groupId}/posts/{postId}/comments/{commentId}` | `deleteComment` |

## SearchController (3)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/search/users` | `users` |
| GET | `/api/search/groups` | `groups` |
| GET | `/api/search/meetings` | `meetings` |

## UserController (8)

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/api/users/me` | `getMe` |
| PATCH | `/api/users/me` | `updateMe` |
| DELETE | `/api/users/me` | `deleteMe` |
| GET | `/api/users/{username}` | `getByUsername` |
| GET | `/api/users/{username}/availability` | `getAvailability` |
| GET | `/api/users/{username}/schedule` | `getSchedule` |
| GET | `/api/users/me/availability` | `getAvailability` |
| PUT | `/api/users/me/availability` | `updateAvailability` |
