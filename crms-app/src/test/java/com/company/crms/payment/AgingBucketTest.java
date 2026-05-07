package com.company.crms.payment;

import com.company.crms.payment.service.impl.AgingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgingBucketTest {

    private final LocalDate today = LocalDate.of(2026, 5, 1);

    @Test
    void undue_when_planDate_in_future() {
        assertEquals(0, AgingServiceImpl.bucketIndex(today, today.plusDays(10)));
        assertEquals(0, AgingServiceImpl.bucketIndex(today, today));
    }

    @Test
    void bucket_0_30() {
        assertEquals(1, AgingServiceImpl.bucketIndex(today, today.minusDays(1)));
        assertEquals(1, AgingServiceImpl.bucketIndex(today, today.minusDays(30)));
    }

    @Test
    void bucket_31_60() {
        assertEquals(2, AgingServiceImpl.bucketIndex(today, today.minusDays(31)));
        assertEquals(2, AgingServiceImpl.bucketIndex(today, today.minusDays(60)));
    }

    @Test
    void bucket_61_90() {
        assertEquals(3, AgingServiceImpl.bucketIndex(today, today.minusDays(61)));
        assertEquals(3, AgingServiceImpl.bucketIndex(today, today.minusDays(90)));
    }

    @Test
    void bucket_90_plus() {
        assertEquals(4, AgingServiceImpl.bucketIndex(today, today.minusDays(91)));
        assertEquals(4, AgingServiceImpl.bucketIndex(today, today.minusYears(2)));
    }
}
