package com.api.v1.inventory.common.util;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.model.enumerate.ReportPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReportDateRangeTest {

    @Test
    @DisplayName("Should resolve TODAY period correctly")
    void shouldResolveToday() {
        ReportDateRange range = ReportDateRange.resolve(ReportPeriod.TODAY, null, null, "UTC");
        assertNotNull(range.getFromDate());
        assertNotNull(range.getToDate());
        assertTrue(range.getFromDate().isBefore(range.getToDate()));
    }

    @Test
    @DisplayName("Should resolve THIS_WEEK period correctly")
    void shouldResolveThisWeek() {
        ReportDateRange range = ReportDateRange.resolve(ReportPeriod.THIS_WEEK, null, null, "UTC");
        assertNotNull(range.getFromDate());
        assertNotNull(range.getToDate());
        assertTrue(range.getFromDate().isBefore(range.getToDate()));
    }

    @Test
    @DisplayName("Should resolve THIS_MONTH period correctly")
    void shouldResolveThisMonth() {
        ReportDateRange range = ReportDateRange.resolve(ReportPeriod.THIS_MONTH, null, null, "UTC");
        assertNotNull(range.getFromDate());
        assertNotNull(range.getToDate());
        assertTrue(range.getFromDate().isBefore(range.getToDate()));
    }

    @Test
    @DisplayName("Should resolve THIS_YEAR period correctly")
    void shouldResolveThisYear() {
        ReportDateRange range = ReportDateRange.resolve(ReportPeriod.THIS_YEAR, null, null, "UTC");
        assertNotNull(range.getFromDate());
        assertNotNull(range.getToDate());
        assertTrue(range.getFromDate().isBefore(range.getToDate()));
    }

    @Test
    @DisplayName("Should resolve CUSTOM period with valid dates")
    void shouldResolveCustomValid() {
        Instant from = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant to = Instant.now();
        ReportDateRange range = ReportDateRange.resolve(ReportPeriod.CUSTOM, from, to, null);
        assertEquals(from, range.getFromDate());
        assertEquals(to, range.getToDate());
    }

    @Test
    @DisplayName("Should throw BadRequestException if fromDate is after toDate in CUSTOM period")
    void shouldThrowWhenFromDateAfterToDate() {
        Instant from = Instant.now();
        Instant to = Instant.now().minus(1, ChronoUnit.DAYS);
        assertThrows(BadRequestException.class, () ->
                ReportDateRange.resolve(ReportPeriod.CUSTOM, from, to, null)
        );
    }

    @Test
    @DisplayName("Should throw BadRequestException on invalid timezone")
    void shouldThrowOnInvalidTimezone() {
        assertThrows(BadRequestException.class, () ->
                ReportDateRange.resolve(ReportPeriod.TODAY, null, null, "INVALID_ZONE_XYZ")
        );
    }
}
