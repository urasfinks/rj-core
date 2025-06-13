package ru.jamsys.core.extension.expiration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpirationMsTest {

    static class TestExpirationMs implements ExpirationMs {
        private final long lastActivityMs;
        private final long keepAliveMs;
        private Long stopTimeMs = null;

        public TestExpirationMs(long lastActivityMs, long keepAliveMs) {
            this.lastActivityMs = lastActivityMs;
            this.keepAliveMs = keepAliveMs;
        }

        @Override
        public long getLastActivityMs() {
            return lastActivityMs;
        }

        @Override
        public long getInactivityTimeoutMs() {
            return keepAliveMs;
        }

        @Override
        public void setStopTimeMs(Long timeMs) {
            this.stopTimeMs = timeMs;
        }

        @Override
        public Long getStopTimeMs() {
            return stopTimeMs;
        }

    }

    private TestExpirationMs exp;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        exp = new TestExpirationMs(now - 5000, 3000); // lastActivity 5s ago, timeout is 3s
    }

    @Test
    void testIsExpired_True() {
        assertTrue(exp.isExpired(), "Object should be expired (lastActivity + timeout < now)");
    }

    @Test
    void testIsExpired_False() {
        exp = new TestExpirationMs(System.currentTimeMillis(), 5000);
        assertFalse(exp.isExpired(), "Object should NOT be expired (recent activity)");
    }

    @Test
    void testIsExpired_Stopped_False() {
        exp.markStop();
        assertFalse(exp.isExpired(), "Stopped object should not be considered expired");
    }

    @Test
    void testIsExpiredIgnoringMarkStop_TrueEvenIfStopped() {
        // Делай объект заведомо протухшим
        long now = System.currentTimeMillis();
        long lastActivity = now - 5000; // 5 секунд назад
        long keepAlive = 1000; // протухает через 1 сек

        exp = new TestExpirationMs(lastActivity, keepAlive);

        exp.markStop(); // Ставим флаг isStop = true

        assertTrue(
                exp.isExpiredIgnoringStop(now),
                "isExpiredWithoutStop должно вернуть true, игнорируя isStop"
        );
    }

    @Test
    void testExpiryRemainingMs_Positive() {
        exp = new TestExpirationMs(System.currentTimeMillis(), 5000);
        assertTrue(exp.getRemainingMs() > 0, "Remaining time should be positive for active object");
    }

    @Test
    void testExpiryRemainingMs_Stopped() {
        exp.markStop();
        assertEquals(0, exp.getRemainingMs(), "Remaining time should be 0 for stopped object");
    }

    @Test
    void testInactivityTimeMs_Active() {
        long now = System.currentTimeMillis();
        long inactivity = exp.getDurationSinceLastActivityMs(now);
        assertTrue(inactivity >= 5000, "Inactivity time should match time since lastActivity");
    }

    @Test
    void testInactivityTimeMs_Stopped() {
        exp.markStop(exp.getLastActivityMs() + 7000);
        assertEquals(7000, exp.getDurationSinceLastActivityMs(), "Inactivity should be stopTime - lastActivity");
    }

    @Test
    void testMarkStopAndIsStopped() {
        assertFalse(exp.isStopped(), "Initially should not be stopped");
        exp.markStop();
        assertTrue(exp.isStopped(), "After stop() call, isStop() should be true");
    }

    @Test
    void testGetExpirationTimeMs() {
        long expected = exp.getLastActivityMs() + exp.getInactivityTimeoutMs();
        assertEquals(expected, exp.getExpirationTimeMs(), "ExpiredMs should be lastActivity + timeout");
    }

    @Test
    void getLastActivityFormatted_ShouldReturnFormattedTime() {
        long now = System.currentTimeMillis();
        TestExpirationMs exp = new TestExpirationMs(now, 10_000);
        String result = exp.getLastActivityFormatted();

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    void getExpirationFormatted_ShouldReturnFormattedExpirationTime() {
        long now = System.currentTimeMillis();
        long timeout = 15_000;
        TestExpirationMs exp = new TestExpirationMs(now, timeout);
        String expirationFormatted = exp.getExpirationFormatted();

        assertNotNull(expirationFormatted);
        assertTrue(expirationFormatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    void getMarkStopTimeFormatted_ShouldReturnNullIfNeverStopped() {
        long now = System.currentTimeMillis();
        TestExpirationMs exp = new TestExpirationMs(now, 10_000);
        assertEquals("-", exp.getStopTimeFormatted());
    }

    @Test
    void getMarkStopTimeFormatted_ShouldReturnFormattedTime() {
        long now = System.currentTimeMillis();
        TestExpirationMs exp = new TestExpirationMs(now - 10_000, 20_000);
        exp.setStopTimeMs(now);
        String stopTimeFormatted = exp.getStopTimeFormatted();

        assertNotNull(stopTimeFormatted);
        assertTrue(stopTimeFormatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

}
