package com.meetingplanner.domains.email.repository;

import com.meetingplanner.domains.email.model.EmailOutboxEntity;
import com.meetingplanner.domains.email.model.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntity, Long> {

    List<EmailOutboxEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            EmailStatus status, OffsetDateTime now);

    @Transactional
    @Modifying
    @Query("""
            delete from EmailOutboxEntity e
            where (e.status = com.meetingplanner.domains.email.model.EmailStatus.SENT and e.sentAt < :sentBefore)
               or (e.status = com.meetingplanner.domains.email.model.EmailStatus.FAILED and e.createdAt < :failedBefore)
            """)
    int deleteSentAndFailed(@Param("sentBefore") OffsetDateTime sentBefore,
                            @Param("failedBefore") OffsetDateTime failedBefore);
}
