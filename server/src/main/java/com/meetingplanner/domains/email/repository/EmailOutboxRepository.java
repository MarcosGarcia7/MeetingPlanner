package com.meetingplanner.domains.email.repository;

import com.meetingplanner.domains.email.model.EmailOutboxEntity;
import com.meetingplanner.domains.email.model.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntity, Long> {

    List<EmailOutboxEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            EmailStatus status, OffsetDateTime now);
}