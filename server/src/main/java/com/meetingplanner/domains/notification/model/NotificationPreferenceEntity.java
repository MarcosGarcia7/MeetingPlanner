package com.meetingplanner.domains.notification.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "group_invites", nullable = false)
    private boolean groupInvites = true;

    @Column(name = "meeting_invites", nullable = false)
    private boolean meetingInvites = true;

    @Column(name = "meeting_reminders", nullable = false)
    private boolean meetingReminders = true;

    @Column(name = "meeting_updates", nullable = false)
    private boolean meetingUpdates = true;

    @Column(name = "friend_requests", nullable = false)
    private boolean friendRequests = true;

    @Column(name = "post_comments", nullable = false)
    private boolean postComments = true;

    @Column(name = "group_posts", nullable = false)
    private boolean groupPosts = true;

    @Column(name = "reminder_lead_minutes", nullable = false)
    private int reminderLeadMinutes = 30;

    public NotificationPreferenceEntity() {
    }

    public Long getUserId() {
        return userId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isGroupInvites() {
        return groupInvites;
    }

    public void setGroupInvites(boolean groupInvites) {
        this.groupInvites = groupInvites;
    }

    public boolean isMeetingInvites() {
        return meetingInvites;
    }

    public void setMeetingInvites(boolean meetingInvites) {
        this.meetingInvites = meetingInvites;
    }

    public boolean isMeetingReminders() {
        return meetingReminders;
    }

    public void setMeetingReminders(boolean meetingReminders) {
        this.meetingReminders = meetingReminders;
    }

    public boolean isMeetingUpdates() {
        return meetingUpdates;
    }

    public void setMeetingUpdates(boolean meetingUpdates) {
        this.meetingUpdates = meetingUpdates;
    }

    public boolean isFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(boolean friendRequests) {
        this.friendRequests = friendRequests;
    }

    public boolean isPostComments() {
        return postComments;
    }

    public void setPostComments(boolean postComments) {
        this.postComments = postComments;
    }

    public boolean isGroupPosts() {
        return groupPosts;
    }

    public void setGroupPosts(boolean groupPosts) {
        this.groupPosts = groupPosts;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(int reminderLeadMinutes) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }
}