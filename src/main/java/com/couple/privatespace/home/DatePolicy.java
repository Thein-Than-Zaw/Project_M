package com.couple.privatespace.home;

import java.time.*;
import java.time.temporal.ChronoUnit;

/** DATE-02 / CUR-DATE-001: calendar days in the couple zone, inclusive Day 1. */
public final class DatePolicy {
    private DatePolicy() { }
    public record Calendar(LocalDate today, long dayNumber, LocalDate anniversary,
            long daysUntilAnniversary, Instant nextMidnight, String timezone) { }
    public static Calendar calendar(LocalDate start, ZoneId zone, Clock clock) {
        var today=LocalDate.now(clock.withZone(zone));
        var anniversary=occurrence(start,today.getYear());
        if(anniversary.isBefore(today)) anniversary=occurrence(start,today.getYear()+1);
        return new Calendar(today,ChronoUnit.DAYS.between(start,today)+1,anniversary,
            ChronoUnit.DAYS.between(today,anniversary),today.plusDays(1).atStartOfDay(zone).toInstant(),zone.getId());
    }
    public static LocalDate occurrence(LocalDate original,int year) {
        // LocalDate.withYear clamps Feb29 to Feb28 and restores Feb29 in leap years.
        return original.withYear(year);
    }
}
