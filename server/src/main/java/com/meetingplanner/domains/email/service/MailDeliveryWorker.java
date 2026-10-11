package com.meetingplanner.domains.email.service;

import com.meetingplanner.domains.email.model.EmailOutboxEntity;
import com.meetingplanner.domains.email.model.EmailStatus;
import com.meetingplanner.domains.email.repository.EmailOutboxRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class MailDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(MailDeliveryWorker.class);

    private static final int MAX_ATTEMPTS = 12;
    private static final long BASE_BACKOFF_SECONDS = 60;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailTemplateRegistry templateRegistry;

    @Value("${app.email.from:meetingplanner@localhost}")
    private String fromAddress;

    public MailDeliveryWorker(ObjectProvider<JavaMailSender> mailSenderProvider,
                               EmailOutboxRepository emailOutboxRepository,
                               EmailTemplateRegistry templateRegistry) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailOutboxRepository = emailOutboxRepository;
        this.templateRegistry = templateRegistry;
    }

    @Scheduled(fixedDelayString = "${app.email.drain-delay-ms:30000}", initialDelay = 15000)
    @Transactional
    public void drain() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }

        List<EmailOutboxEntity> pending = emailOutboxRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        EmailStatus.PENDING, OffsetDateTime.now());

        for (EmailOutboxEntity outbox : pending) {
            try {
                MimeMessage message = templateRegistry.render(mailSender, outbox, fromAddress);
                mailSender.send(message);
                outbox.setStatus(EmailStatus.SENT);
                outbox.setSentAt(OffsetDateTime.now());
                outbox.setProviderMessageId(message.getMessageID());
            } catch (Exception e) {
                int attempts = outbox.getAttempts() + 1;
                outbox.setAttempts(attempts);
                outbox.setLastError(String.valueOf(e.getMessage()));
                outbox.setStatus(attempts >= MAX_ATTEMPTS ? EmailStatus.FAILED : EmailStatus.PENDING);
                outbox.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSeconds(attempts)));
                log.warn("event=email_send_failed outbox_id={} attempts={} error={}",
                        outbox.getId(), attempts, e.getMessage());
            }
            emailOutboxRepository.save(outbox);
        }
    }

    private long backoffSeconds(int attempts) {
        long backoff = BASE_BACKOFF_SECONDS * (1L << Math.min(attempts - 1, 8));
        return Math.min(backoff, 4 * 60 * 60);
    }
}