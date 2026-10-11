package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class MeetingCleanupWorker {

    private static final Logger log = LoggerFactory.getLogger(MeetingCleanupWorker.class);
    private static final int RETENTION_DAYS = 30;

    private final MeetingRepository meetingRepository;

    public MeetingCleanupWorker(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Scheduled(cron = "${app.meeting.cleanup-cron:0 0 3 * * *}", zone = "UTC")
    @Transactional
    public void cleanupOldMeetings() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = meetingRepository.deleteMeetingsEndedBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} meetings ended before {}", deleted, cutoff);
        }
    }
}
