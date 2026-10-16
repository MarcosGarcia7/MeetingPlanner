package com.meetingplanner.domains.meeting.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MeetingTimeChangeExpiryWorker {

    private final MeetingTimeChangeService timeChangeService;

    public MeetingTimeChangeExpiryWorker(MeetingTimeChangeService timeChangeService) {
        this.timeChangeService = timeChangeService;
    }

    @Scheduled(fixedDelayString = "${app.meeting.time-change-expiry-delay-ms:300000}", initialDelay = 30000)
    public void expireStaleProposals() {
        timeChangeService.expireStale();
    }
}
