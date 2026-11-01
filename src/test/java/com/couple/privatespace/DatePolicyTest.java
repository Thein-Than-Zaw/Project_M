package com.couple.privatespace;

import com.couple.privatespace.home.DatePolicy;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatePolicyTest {
    /** CUR-DATE-001: independent expected values, Myanmar midnight is 17:30 UTC. */
    @Test void inclusiveDayOneAndMyanmarMidnight() {
        var zone=ZoneId.of("Asia/Yangon");var start=LocalDate.parse("2026-10-31");
        var before=DatePolicy.calendar(start,zone,Clock.fixed(Instant.parse("2026-10-31T17:29:59Z"),ZoneOffset.UTC));
        assertEquals(1,before.dayNumber());assertEquals(0,before.daysUntilAnniversary());
        assertEquals(Instant.parse("2026-10-31T17:30:00Z"),before.nextMidnight());
        var after=DatePolicy.calendar(start,zone,Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"),ZoneOffset.UTC));
        assertEquals(2,after.dayNumber());assertEquals(LocalDate.parse("2026-11-01"),after.today());
        var year=DatePolicy.calendar(start,zone,Clock.fixed(Instant.parse("2027-10-31T00:00:00Z"),ZoneOffset.UTC));
        assertEquals(366,year.dayNumber());
    }
    @Test void leapAnniversaryClampsAndRestoresOriginalDay() {
        var start=LocalDate.parse("2024-02-29");
        assertEquals(LocalDate.parse("2025-02-28"),DatePolicy.occurrence(start,2025));
        assertEquals(LocalDate.parse("2028-02-29"),DatePolicy.occurrence(start,2028));
        var on=DatePolicy.calendar(start,ZoneId.of("Asia/Yangon"),Clock.fixed(Instant.parse("2025-02-28T00:00:00Z"),ZoneOffset.UTC));
        assertEquals(366,on.dayNumber());assertEquals(0,on.daysUntilAnniversary());
        var passed=DatePolicy.calendar(start,ZoneId.of("Asia/Yangon"),Clock.fixed(Instant.parse("2027-03-01T00:00:00Z"),ZoneOffset.UTC));
        assertEquals(LocalDate.parse("2028-02-29"),passed.anniversary());
        var newYear=DatePolicy.calendar(LocalDate.parse("2025-12-31"),ZoneId.of("Asia/Yangon"),Clock.fixed(Instant.parse("2025-12-31T17:30:00Z"),ZoneOffset.UTC));
        assertEquals(2,newYear.dayNumber());assertEquals(LocalDate.parse("2026-12-31"),newYear.anniversary());
    }
}
