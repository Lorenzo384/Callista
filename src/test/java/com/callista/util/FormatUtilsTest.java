package com.callista.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatUtilsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2                     | 2",
            "-3                    | -3",
            "0.5                   | 0.5",
            "12.909297426825681    | 12.90929743",
            "3.141592653589793     | 3.141592654",
            "1.0E-16               | 0",
            "-0.0                  | 0",
            "0.000123              | 0.000123",
            "1.5E15                | 1.5E15",
            "2.5E-8                | 2.5E-8",
    })
    void formatsNumbersForDisplay(double value, String expected) {
        assertEquals(expected, FormatUtils.formatNumber(value));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NaN       | indefinido",
            "Infinity  | ∞",
            "-Infinity | -∞",
    })
    void formatsSpecialValues(double value, String expected) {
        assertEquals(expected, FormatUtils.formatNumber(value));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1.0471975511965976  | 1.047",
            "0.8660254037844386  | 0.866",
            "12.909297426825681  | 12.91",
            "2                   | 2",
    })
    void formatsShortNumbersForLabels(double value, String expected) {
        assertEquals(expected, FormatUtils.formatShort(value));
    }
}
