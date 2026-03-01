package com.itxiop.tech.supplier.sandbox.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoMinUniqueTest {

    @Test
    void captures_two_lowest_unique_with_ties() {
        TwoMinUnique mins = new TwoMinUnique();

        mins.accept(200);
        mins.accept(200);
        mins.accept(210);
        mins.accept(210);
        mins.accept(250);

        assertTrue(mins.isBonus(200));
        assertTrue(mins.isBonus(210));
        assertFalse(mins.isBonus(250));
    }

    @Test
    void if_only_one_unique_then_only_that_one_is_bonus() {
        TwoMinUnique mins = new TwoMinUnique();
        mins.accept(200);
        mins.accept(200);

        assertTrue(mins.isBonus(200));
        assertFalse(mins.isBonus(210));
    }
}