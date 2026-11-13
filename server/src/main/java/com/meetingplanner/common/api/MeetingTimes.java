package com.meetingplanner.common.api;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Human-readable meeting times for emails, e.g. "Tue, Oct 27, 2026, 2:00 – 3:30 PM CDT", shown in the
 * recipient's time zone.
 */
public final class MeetingTimes {

    private static final DateTimeFormatter DAY_WITH_YEAR = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, ", Locale.US);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d, ", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm", Locale.US);
    private static final DateTimeFormatter TIME_WITH_PERIOD = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter ZONE = DateTimeFormatter.ofPattern("z", Locale.US);

    // A region ID rather than ZoneOffset.UTC so the "z" pattern prints "UTC", not "Z".
    private static final ZoneId UTC = ZoneId.of("UTC");

    private MeetingTimes() {
    }

    /** The first valid time zone among the candidates (e.g. a user's profile zone), else UTC. */
    public static ZoneId zoneFor(String... timezones) {
        for (String timezone : timezones) {
            if (timezone == null || timezone.isBlank()) {
                continue;
            }
            try {
                return ZoneId.of(timezone.trim());
            } catch (DateTimeException e) {
                // Skip values that are no longer valid zone IDs.
            }
        }
        return UTC;
    }

    /**
     * "Tue, Oct 27, 2026, 2:00 – 3:30 PM CDT"; "Tue, Oct 27, 2026, 11:00 PM – Wed, Oct 28, 12:30 AM CDT"
     * when the meeting crosses midnight; "Tue, Oct 27, 2026, 2:00 PM CDT" without an end.
     */
    public static String format(OffsetDateTime start, OffsetDateTime end, ZoneId zone) {
        ZonedDateTime from = start.atZoneSameInstant(zone);
        StringBuilder text = new StringBuilder(DAY_WITH_YEAR.format(from));
        if (end == null) {
            text.append(TIME_WITH_PERIOD.format(from));
        } else {
            ZonedDateTime to = end.atZoneSameInstant(zone);
            if (!from.toLocalDate().equals(to.toLocalDate())) {
                text.append(TIME_WITH_PERIOD.format(from)).append(" – ").append(DAY.format(to));
            } else if (from.getHour() / 12 == to.getHour() / 12) {
                text.append(TIME.format(from)).append(" – ");
            } else {
                text.append(TIME_WITH_PERIOD.format(from)).append(" – ");
            }
            text.append(TIME_WITH_PERIOD.format(to));
            from = to;
        }
        return text.append(' ').append(ZONE.format(from)).toString();
    }
}
