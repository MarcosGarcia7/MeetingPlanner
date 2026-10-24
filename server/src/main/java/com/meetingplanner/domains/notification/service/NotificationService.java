package com.meetingplanner.domains.notification.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.meetingplanner.domains.email.model.EmailOutboxEntity;
import com.meetingplanner.domains.email.repository.EmailOutboxRepository;
import com.meetingplanner.domains.notification.model.NotificationEntity;
import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import com.meetingplanner.domains.notification.repository.NotificationRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final EmailOutboxRepository emailOutboxRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationPreferenceRepository preferenceRepository,
                               NotificationRepository notificationRepository,
                               EmailOutboxRepository emailOutboxRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
        this.emailOutboxRepository = emailOutboxRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void push(long userId, String type, String category, String template, Map<String, String> vars) {
        NotificationPreferenceEntity prefs = preferencesFor(userId);
        if (!categoryEnabled(prefs, category)) {
            return;
        }

        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setType(type);
        notification.setPayload(json(vars));
        notificationRepository.save(notification);

        if (prefs.isEmailEnabled()) {
            queueEmail(user, template, vars);
        }
    }

    @Transactional
    public void queueSecurityEmail(UserEntity user, String template, Map<String, String> vars) {
        queueEmail(user, template, vars);
    }

    @Transactional
    public void queueEmailTo(String email, String template, Map<String, String> vars) {
        EmailOutboxEntity outbox = new EmailOutboxEntity();
        outbox.setRecipientEmail(email);
        outbox.setTemplate(template);
        outbox.setPayload(json(vars));
        emailOutboxRepository.save(outbox);
    }

    @Transactional
    public void queueEmailToForUser(UserEntity user, String email, String template, Map<String, String> vars, String category) {
        if (user != null) {
            NotificationPreferenceEntity prefs = preferencesFor(user.getId());
            if (!prefs.isEmailEnabled()) {
                return;
            }
            if (category != null && !categoryEnabled(prefs, category)) {
                return;
            }
            queueEmail(user, template, vars);
            return;
        }
        queueEmailTo(email, template, vars);
    }

    private void queueEmail(UserEntity user, String template, Map<String, String> vars) {
        EmailOutboxEntity outbox = new EmailOutboxEntity();
        outbox.setRecipientUser(user);
        outbox.setRecipientEmail(user.getEmail());
        outbox.setTemplate(template);
        outbox.setPayload(json(vars));
        emailOutboxRepository.save(outbox);
    }

    private NotificationPreferenceEntity preferencesFor(long userId) {
        return preferenceRepository.findOrCreate(userId);
    }

    private boolean categoryEnabled(NotificationPreferenceEntity prefs, String category) {
        return switch (category) {
            case "group_invites" -> prefs.isGroupInvites();
            case "meeting_invites" -> prefs.isMeetingInvites();
            case "meeting_reminders" -> prefs.isMeetingReminders();
            case "meeting_updates" -> prefs.isMeetingUpdates();
            case "friend_requests" -> prefs.isFriendRequests();
            case "post_comments" -> prefs.isPostComments();
            case "group_posts" -> prefs.isGroupPosts();
            default -> true;
        };
    }

    private String json(Map<String, String> vars) {
        try {
            return objectMapper.writeValueAsString(vars == null ? Map.of() : vars);
        } catch (JacksonException e) {
            log.warn("Failed to serialize notification payload", e);
            return "{}";
        }
    }
}