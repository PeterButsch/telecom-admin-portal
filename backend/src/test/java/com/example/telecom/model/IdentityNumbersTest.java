package com.example.telecom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdentityNumbersTest {

    @Test
    void acceptsPublishedExamples() {
        assertTrue(IdentityNumbers.isValidNricOrFin("S1234567D"));
        assertTrue(IdentityNumbers.isValidNricOrFin("t0312345b"));
        assertTrue(IdentityNumbers.isValidNricOrFin("G1234567X"));
        assertTrue(IdentityNumbers.isValidNricOrFin("M1234567K"));
        assertEquals('D', IdentityNumbers.checkLetter('S', "1234567"));
        assertEquals('K', IdentityNumbers.checkLetter('M', "1234567"));
    }

    @Test
    void rejectsWrongCheckLettersAndFormats() {
        assertFalse(IdentityNumbers.isValidNricOrFin("S1234567A"));
        assertFalse(IdentityNumbers.isValidNricOrFin("A1234567D"));
        assertFalse(IdentityNumbers.isValidNricOrFin("S123456D"));
        assertFalse(IdentityNumbers.isValidNricOrFin(null));
    }

    @Test
    void distinguishesNricFromFin() {
        assertTrue(IdentityNumbers.isNricPrefix("S1234567D"));
        assertTrue(IdentityNumbers.isNricPrefix("T1234567J"));
        assertFalse(IdentityNumbers.isNricPrefix("G1234567X"));
    }

    @Test
    void masksAllButTheLastFourCharacters() {
        assertEquals("*****567D", IdentityNumbers.mask("s1234567d"));
        assertEquals("***", IdentityNumbers.mask("ABC"));
        assertNull(IdentityNumbers.mask(null));
    }
}
