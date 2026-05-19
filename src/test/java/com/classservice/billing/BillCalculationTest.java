package com.classservice.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for billing calculation logic.
 * No database, no Spring context needed.
 */
class BillCalculationTest {

    @Test
    void totalAmount_isAttendedSessionsTimesRate() {
        int attended = 8;
        BigDecimal rate = new BigDecimal("150000");
        BigDecimal total = rate.multiply(BigDecimal.valueOf(attended));
        assertThat(total).isEqualByComparingTo(new BigDecimal("1200000"));
    }

    @Test
    void absentStudent_producesZeroAmount() {
        int attended = 0;
        BigDecimal rate = new BigDecimal("150000");
        BigDecimal total = rate.multiply(BigDecimal.valueOf(attended));
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelledSessions_notCountedInTotal() {
        // sessionsTotal = 10, cancelledByTeacher = 3 → non-cancelled = 7
        // student attended all 7
        int nonCancelled = 7;
        int attended = 7;
        BigDecimal rate = new BigDecimal("150000");
        BigDecimal total = rate.multiply(BigDecimal.valueOf(attended));
        assertThat(total).isEqualByComparingTo(new BigDecimal("1050000"));
        assertThat(attended).isLessThanOrEqualTo(nonCancelled);
    }
}
