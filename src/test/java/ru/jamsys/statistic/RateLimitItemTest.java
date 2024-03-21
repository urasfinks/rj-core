package ru.jamsys.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RateLimitItemTest {

    @Test
    void checkMax() {
        RateLimitItem rateLimitItem = new RateLimitItem();
        rateLimitItem.setMax(2);
        Assertions.assertTrue(rateLimitItem.checkMax(1));
        Assertions.assertTrue(rateLimitItem.checkMax(2));
        Assertions.assertFalse(rateLimitItem.checkMax(3));
        rateLimitItem.setMax(-1);
        Assertions.assertTrue(rateLimitItem.checkMax(3));
        Assertions.assertTrue(rateLimitItem.checkMax(3));
        // Эту функцию будут вызывать при установки новых значений, низя
        Assertions.assertTrue(rateLimitItem.checkMax(-1));
        Assertions.assertFalse(rateLimitItem.checkMax(-2));
        Assertions.assertFalse(rateLimitItem.checkMax(-3));
    }

    @Test
    void checkTps() {
        RateLimitItem rateLimitItem = new RateLimitItem();
        rateLimitItem.setMax(2);
        Assertions.assertTrue(rateLimitItem.checkTps());
        Assertions.assertTrue(rateLimitItem.checkTps());
        Assertions.assertFalse(rateLimitItem.checkTps());
    }
}