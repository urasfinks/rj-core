package ru.jamsys.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.rate.limit.RateLimitMax;
import ru.jamsys.rate.limit.RateLimitTps;

class RateLimitManagerItemTest {

    @Test
    void checkMax() {
        RateLimitMax rateLimitItem = new RateLimitMax();
        rateLimitItem.setMax(2);
        Assertions.assertTrue(rateLimitItem.checkLimit(1));
        Assertions.assertTrue(rateLimitItem.checkLimit(2));
        Assertions.assertFalse(rateLimitItem.checkLimit(3));
        rateLimitItem.setMax(-1);
        Assertions.assertTrue(rateLimitItem.checkLimit(3));
        Assertions.assertTrue(rateLimitItem.checkLimit(3));
        // Эту функцию будут вызывать при установки новых значений, низя
        Assertions.assertTrue(rateLimitItem.checkLimit(-1));
        Assertions.assertFalse(rateLimitItem.checkLimit(-2));
        Assertions.assertFalse(rateLimitItem.checkLimit(-3));
    }

    @Test
    void checkTps() {
        RateLimitTps rateLimitItem = new RateLimitTps();
        rateLimitItem.setMax(2);
        Assertions.assertTrue(rateLimitItem.checkTps());
        Assertions.assertTrue(rateLimitItem.checkTps());
        Assertions.assertFalse(rateLimitItem.checkTps());
    }

}