package com.bancoprogramacao.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bancoprogramacao.api.exception.BankBusinessException;
import org.junit.jupiter.api.Test;

class CheckDigitCalculatorTest {

    @Test
    void shouldCalculateTheDigitFromTheAssignmentExample() {
        assertEquals("9", CheckDigitCalculator.calculate("261533"));
    }

    @Test
    void shouldCalculateDigitForASecondAccount() {
        assertEquals("1", CheckDigitCalculator.calculate("4586"));
    }

    @Test
    void shouldRejectAccountNumbersOutsideTheAllowedRange() {
        assertThrows(BankBusinessException.class, () -> CheckDigitCalculator.calculate("12"));
        assertThrows(BankBusinessException.class, () -> CheckDigitCalculator.calculate("12345678"));
    }
}

