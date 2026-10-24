package com.meetingplanner.domains.notification.repository;

import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {

    Optional<NotificationPreferenceEntity> findByUserId(Long userId);

    // Every other column has a DB default. ON CONFLICT keeps concurrent first uses from colliding on the PK.
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into notification_preferences (user_id) values (:userId) on conflict do nothing",
            nativeQuery = true)
    int insertDefaultsIfAbsent(@Param("userId") Long userId);

    // Inserted in SQL rather than via save(): the entity's assigned id makes save() merge, which fails here.
    default NotificationPreferenceEntity findOrCreate(long userId) {
        return findByUserId(userId).orElseGet(() -> {
            insertDefaultsIfAbsent(userId);
            return findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Preferences not created for user " + userId));
        });
    }
}
