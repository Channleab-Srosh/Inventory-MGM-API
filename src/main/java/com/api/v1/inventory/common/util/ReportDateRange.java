package com.api.v1.inventory.common.util;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import lombok.Value;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Value(staticConstructor = "of")
public class ReportDateRange {

    Instant fromDate;
    Instant toDate;

    public static ReportDateRange resolve(ReportPeriod period, Instant fromDate, Instant toDate, String zoneIdString) {
        ZoneId zoneId;
        if (zoneIdString != null && !zoneIdString.trim().isEmpty()) {
            try {
                zoneId = ZoneId.of(zoneIdString.trim());
            } catch (Exception e) {
                throw new BadRequestException("Invalid timezone identifier: " + zoneIdString);
            }
        } else {
            zoneId = ZoneId.systemDefault();
        }

        if (period == null || period == ReportPeriod.CUSTOM) {
            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                throw new BadRequestException("fromDate must be before or equal to toDate");
            }
            return ReportDateRange.of(fromDate, toDate);
        }

        LocalDate today = LocalDate.now(zoneId);

        return switch (period) {
            case TODAY -> {
                Instant start = today.atStartOfDay(zoneId).toInstant();
                Instant end = today.plusDays(1).atStartOfDay(zoneId).minus(1, ChronoUnit.MILLIS).toInstant();
                yield ReportDateRange.of(start, end);
            }
            case THIS_WEEK -> {
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                Instant start = startOfWeek.atStartOfDay(zoneId).toInstant();
                Instant end = startOfWeek.plusDays(7).atStartOfDay(zoneId).minus(1, ChronoUnit.MILLIS).toInstant();
                yield ReportDateRange.of(start, end);
            }
            case THIS_MONTH -> {
                LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
                Instant start = startOfMonth.atStartOfDay(zoneId).toInstant();
                Instant end = startOfMonth.plusMonths(1).atStartOfDay(zoneId).minus(1, ChronoUnit.MILLIS).toInstant();
                yield ReportDateRange.of(start, end);
            }
            case THIS_YEAR -> {
                LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
                Instant start = startOfYear.atStartOfDay(zoneId).toInstant();
                Instant end = startOfYear.plusYears(1).atStartOfDay(zoneId).minus(1, ChronoUnit.MILLIS).toInstant();
                yield ReportDateRange.of(start, end);
            }
            default -> ReportDateRange.of(fromDate, toDate);
        };
    }
}
