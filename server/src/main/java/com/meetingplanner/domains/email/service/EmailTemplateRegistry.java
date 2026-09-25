package com.meetingplanner.domains.email.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.meetingplanner.domains.email.model.EmailOutboxEntity;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmailTemplateRegistry {

    private final ObjectMapper objectMapper;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String baseUrl;

    public EmailTemplateRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MimeMessage render(JavaMailSender sender, EmailOutboxEntity outbox, String from) {
        Map<String, String> vars = parsePayload(outbox.getPayload());
        vars.put("app_url", baseUrl);

        Template template = template(outbox.getTemplate());
        String subject = substitute(template.subject, vars);
        String text = substitute(template.body, vars);

        try {
            MimeMessage message = sender.createMimeMessage();
            message.setFrom(from);
            message.setRecipients(Message.RecipientType.TO, outbox.getRecipientEmail());
            message.setSubject(subject, "UTF-8");
            message.setText(text, "UTF-8");
            return message;
        } catch (MessagingException e) {
            throw new MailException("Failed to build email", e) {
            };
        }
    }

    private Template template(String name) {
        return switch (name) {
            case "EMAIL_VERIFY" -> new Template(
                    "Verify your email address",
                    "Hi {{name}},\n\nPlease confirm this email address by clicking the link below:\n{{link}}\n\nThis link expires in 24 hours.\n\n- {{app_url}}");
            case "PASSWORD_RESET" -> new Template(
                    "Reset your password",
                    "Hi {{name}},\n\nWe received a request to reset your password. Click the link below to choose a new one:\n{{link}}\n\nIf you did not request this, you can safely ignore this email.\n\n- {{app_url}}");
            case "EMAIL_CHANGE" -> new Template(
                    "Confirm your new email address",
                    "Hi {{name}},\n\nConfirm your new email address by clicking the link below:\n{{link}}\n\nThis link expires in 24 hours.\n\n- {{app_url}}");
            case "GROUP_INVITE" -> new Template(
                    "{{sender}} invited you to {{group}}",
                    "Hi {{name}},\n\n{{sender}} invited you to join the group \"{{group}}\".\nOpen the invite:\n{{link}}\n\n- {{app_url}}");
            case "MEETING_INVITE" -> new Template(
                    "{{sender}} invited you to a meeting",
                    "Hi {{name}},\n\n{{sender}} invited you to a meeting.\nOpen the invite:\n{{link}}\n\n- {{app_url}}");
            case "MEETING_REMINDER" -> new Template(
                    "Reminder: {{title}} starts soon",
                    "Hi {{name}},\n\nA quick reminder that \"{{title}}\" starts at {{start_time}}.\n\n- {{app_url}}");
            case "FRIEND_REQUEST" -> new Template(
                    "{{requester}} sent you a friend request",
                    "Hi {{name}},\n\n{{requester}} sent you a friend request.\nOpen it:\n{{link}}\n\n- {{app_url}}");
            case "FRIEND_REQUEST_ACCEPTED" -> new Template(
                    "{{acceptor}} accepted your friend request",
                    "Hi {{name}},\n\n{{acceptor}} accepted your friend request.\n\n- {{app_url}}");
            case "GROUP_POST" -> new Template(
                    "{{author}} posted in {{group}}",
                    "Hi {{name}},\n\n{{author}} posted in the group \"{{group}}\":\n\n{{body}}\n\n{{link}}\n\n- {{app_url}}");
            case "POST_COMMENT" -> new Template(
                    "{{author}} commented on your post",
                    "Hi {{name}},\n\n{{author}} commented on your post:\n\n{{body}}\n\n{{link}}\n\n- {{app_url}}");
            default -> new Template(
                    "Notification from MeetingPlanner",
                    "Hi {{name}},\n\n{{link}}\n\n- {{app_url}}");
        };
    }

    private Map<String, String> parsePayload(String payload) {
        try {
            if (!StringUtils.hasText(payload)) {
                return new HashMap<>();
            }
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String substitute(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private record Template(String subject, String body) {
    }
}