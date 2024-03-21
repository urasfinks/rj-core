package ru.jamsys.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.component.RateLimit;

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

    @Test
    void checkEquals() {
        RateLimit.ComplexKey complexKey1 = new RateLimit.ComplexKey(RateLimitGroup.OTHER, RateLimitGroup.class, "test");
        RateLimit.ComplexKey complexKey2 = new RateLimit.ComplexKey(RateLimitGroup.OTHER, RateLimitGroup.class, "test");
        Assertions.assertEquals(complexKey2, complexKey1);

        RateLimit.ComplexKey complexKey3 = new RateLimit.ComplexKey(RateLimitGroup.OTHER, RateLimitGroup.class, "test1");
        Assertions.assertNotEquals(complexKey3, complexKey1);

        RateLimit.ComplexKey complexKey4 = new RateLimit.ComplexKey(RateLimitGroup.BROKER, RateLimitGroup.class, "test");
        Assertions.assertNotEquals(complexKey4, complexKey1);
    }
}