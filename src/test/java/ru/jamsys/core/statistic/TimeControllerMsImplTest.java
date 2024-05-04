package ru.jamsys.core.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.statistic.time.TimeControllerMs;
import ru.jamsys.core.statistic.time.TimeControllerMsImpl;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

class TimeControllerMsImplTest {

    @Test
    void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        TimeControllerMs timeControl = new TimeControllerMsImpl();
        timeControl.setKeepAliveOnInactivityMs(5_000);
        timeControl.setLastActivityMs(curTime);

        Assertions.assertFalse(timeControl.isExpired(curTime + 5_000));
        Assertions.assertTrue(timeControl.isExpired(curTime + 5_001));

        //Мы пока никуда не смещались у нас полный запас
        Assertions.assertEquals(5000, timeControl.getExpiryRemainingMs(curTime));

        Assertions.assertEquals(0, timeControl.getExpiryRemainingMs(curTime + 5_000));
        Assertions.assertEquals(-1, timeControl.getExpiryRemainingMs(curTime + 5_001));

        // Остановим метрику, после остановки время не должно меняться
        timeControl.stop(curTime + 5_000);

        // Теперь время
        Assertions.assertEquals(5_000, timeControl.getOffsetLastActivityMs(curTime + 5_001));
        Assertions.assertEquals(5_000, timeControl.getOffsetLastActivityMs(curTime + 6_001));
        Assertions.assertEquals(5_000, timeControl.getOffsetLastActivityMs(curTime + 7_001));
    }

    @Test
    void testExpired(){
        TimeEnvelopeMs<String> timeEnvelopeMs = new TimeEnvelopeMs<>("Hello world");
        timeEnvelopeMs.setKeepAliveOnInactivityMs(6_000);
        Assertions.assertFalse(timeEnvelopeMs.isExpired());
    }
}