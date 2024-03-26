package ru.jamsys.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.rate.limit.RateLimitMax;
import ru.jamsys.rate.limit.RateLimitTps;

class RateLimitManagerItemTest {

    @Test
    void checkMax() {
        RateLimitMax rateLimitMax = new RateLimitMax();
        rateLimitMax.setMax(2);
        Assertions.assertTrue(rateLimitMax.checkLimit(1));
        Assertions.assertTrue(rateLimitMax.checkLimit(2));
        Assertions.assertFalse(rateLimitMax.checkLimit(3));
        rateLimitMax.setMax(-1);
        Assertions.assertTrue(rateLimitMax.checkLimit(3));
        Assertions.assertTrue(rateLimitMax.checkLimit(3));
        // Эту функцию будут вызывать при установки новых значений, низя
        Assertions.assertTrue(rateLimitMax.checkLimit(-1));
        Assertions.assertFalse(rateLimitMax.checkLimit(-2));
        Assertions.assertFalse(rateLimitMax.checkLimit(-3));
    }

    @Test
    void checkTps() {
        RateLimitTps rateLimitTps = new RateLimitTps();
        rateLimitTps.setMax(2);
        Assertions.assertTrue(rateLimitTps.checkTps());
        Assertions.assertTrue(rateLimitTps.checkTps());
        Assertions.assertFalse(rateLimitTps.checkTps());
    }

}