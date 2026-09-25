package com.meetingplanner.domains.notification.repository;

import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {

    Optional<NotificationPreferenceEntity> findByUserId(Long userId);
}