package com.educenter.service.impl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExamScoringTest {
    @Test
    void exactFillMatchNormalizesWhitespaceAndCaseWithoutPartialCredit() {
        assertTrue(ExamScoring.exactFillMatch("  HAI   mươi ", List.of("hai mươi")));
        assertFalse(ExamScoring.exactFillMatch("hai mươ", List.of("hai mươi")));
        assertFalse(ExamScoring.exactFillMatch("", List.of("hai mươi")));
    }

    @Test
    void matrixPointsAreDistributedToCentsAndPreserveCellTotal() {
        List<BigDecimal> shares = ExamScoring.dividePoints(new BigDecimal("10.00"), 3);
        assertEquals(new BigDecimal("10.00"), shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("0.01"), shares.get(0).subtract(shares.get(1)));
        assertThrows(IllegalArgumentException.class, () -> ExamScoring.dividePoints(BigDecimal.ZERO, 3));
    }
}
