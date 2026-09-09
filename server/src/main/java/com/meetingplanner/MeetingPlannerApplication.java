package com.meetingplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingPlannerApplication {
    public static void main(String[]args) {
        SpringApplication.run(MeetingPlannerApplication.class, args);
    }

}