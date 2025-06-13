package ru.jamsys.core.extension.expiration.mutable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpirationMsMutableTest {

    static class TestExpirationMsMutable implements ExpirationMsMutable {
        private long lastActivity;
        private long timeout;
        private Long stopTime;

        @Override
        public void setInactivityTimeoutMs(long timeMs) {
            this.timeout = timeMs;
        }

        @Override
        public void setLastActivityMs(long timeMs) {
            this.lastActivity = timeMs;
        }

        @Override
        public long getLastActivityMs() {
            return lastActivity;
        }

        @Override
        public long getInactivityTimeoutMs() {
            return timeout;
        }

        @Override
        public void setStopTimeMs(Long timeMs) {
            this.stopTime = timeMs;
        }

        @Override
        public Long getStopTimeMs() {
            return stopTime;
        }
    }

    @Test
    void markActive_ShouldSetCurrentTimeAsLastActivity() {
        TestExpirationMsMutable exp = new TestExpirationMsMutable();
        long before = System.currentTimeMillis();
        exp.markActive();
        long after = System.currentTimeMillis();

        assertTrue(exp.getLastActivityMs() >= before);
        assertTrue(exp.getLastActivityMs() <= after);
    }

    @Test
    void setInactivityTimeoutSec_ShouldConvertToMilliseconds() {
        TestExpirationMsMutable exp = new TestExpirationMsMutable();
        exp.setInactivityTimeoutSec(5);
        assertEquals(5_000, exp.getInactivityTimeoutMs());
    }

    @Test
    void setInactivityTimeoutMin_ShouldConvertToMilliseconds() {
        TestExpirationMsMutable exp = new TestExpirationMsMutable();
        exp.setInactivityTimeoutMin(2);
        assertEquals(120_000, exp.getInactivityTimeoutMs());
    }

    @Test
    void setLastActivityMs_ShouldSetCorrectValue() {
        TestExpirationMsMutable exp = new TestExpirationMsMutable();
        exp.setLastActivityMs(123456789L);
        assertEquals(123456789L, exp.getLastActivityMs());
    }
}