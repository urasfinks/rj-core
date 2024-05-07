package ru.jamsys.core.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutable;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableImpl;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutable;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;

class ExpiredMsMutableImplTest {

    @Test
    void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        ExpiredMsMutable timeControl = new ExpiredMsMutableImpl();
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
        Assertions.assertEquals(5_000, timeControl.getInactivityTimeMs(curTime + 5_001));
        Assertions.assertEquals(5_000, timeControl.getInactivityTimeMs(curTime + 6_001));
        Assertions.assertEquals(5_000, timeControl.getInactivityTimeMs(curTime + 7_001));
    }

    @Test
    void testExpired(){
        ExpiredMsMutableEnvelope<String> expiredMsMutableEnvelope = new ExpiredMsMutableEnvelope<>("Hello world");
        expiredMsMutableEnvelope.setKeepAliveOnInactivityMs(6_000);
        Assertions.assertFalse(expiredMsMutableEnvelope.isExpired());
    }

    @Test
    void testInstanceOf(){
        ExpiredMsImmutableImpl timeControllerMsImmutable = new ExpiredMsImmutableImpl(6_000);
        ExpiredMsMutableImpl timeControllerMsMutable = new ExpiredMsMutableImpl();
        // Что бы случайно не накосячить в наследовании интерфейсов, сейчас это невозможно, но легко можно сделать обратное
        Assertions.assertFalse(timeControllerMsImmutable instanceof ExpiredMsMutable);
        Assertions.assertFalse(timeControllerMsMutable instanceof ExpiredMsImmutable);

    }
}