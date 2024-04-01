package ru.jamsys.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeControllerImplTest {

    @Test
    void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        TimeController timeControl = new TimeControllerImpl();
        timeControl.setKeepAliveOnInactivityMs(5_000);
        timeControl.setLastActivity(curTime);

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


}