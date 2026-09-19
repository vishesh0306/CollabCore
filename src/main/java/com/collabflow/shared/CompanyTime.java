package com.collabflow.shared;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The company works in one time zone, so "today" and "overdue" mean the same for everyone.
 * Times are still stored in UTC; only calendar dates use this zone.
 */
public final class CompanyTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private CompanyTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
