package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutable;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableImpl;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutableEnvelope;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutableImpl;

// IO time: 9ms
// COMPUTE time: 8ms

class ExpirationListMsMutableImplTest {

    @Test
    void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        ExpirationMsMutable timeControl = new ExpirationMsMutableImpl();
        timeControl.setInactivityTimeoutMs(5_000);
        timeControl.setLastActivityMs(curTime);

        Assertions.assertFalse(timeControl.isExpired(curTime + 5_000));
        Assertions.assertTrue(timeControl.isExpired(curTime + 5_001));

        //Мы пока никуда не смещались у нас полный запас
        Assertions.assertEquals(5000, timeControl.getRemainingMs(curTime));

        Assertions.assertEquals(0, timeControl.getRemainingMs(curTime + 5_000));
        Assertions.assertEquals(-1, timeControl.getRemainingMs(curTime + 5_001));

        // Остановим метрику, после остановки время не должно меняться
        timeControl.markStop(curTime + 5_000);

        // Теперь время
        Assertions.assertEquals(5_000, timeControl.getDurationSinceLastActivityMs(curTime + 5_001));
        Assertions.assertEquals(5_000, timeControl.getDurationSinceLastActivityMs(curTime + 6_001));
        Assertions.assertEquals(5_000, timeControl.getDurationSinceLastActivityMs(curTime + 7_001));
    }

    @Test
    void testExpired(){
        ExpirationMsMutableEnvelope<String> expirationMsMutableEnvelope = new ExpirationMsMutableEnvelope<>("Hello world");
        expirationMsMutableEnvelope.setInactivityTimeoutMs(6_000);
        Assertions.assertFalse(expirationMsMutableEnvelope.isExpired());
    }

    @SuppressWarnings("all")
    @Test
    void testInstanceOf(){
        ExpirationMsImmutableImpl timeControllerMsImmutable = new ExpirationMsImmutableImpl(6_000);
        ExpirationMsMutableImpl timeControllerMsMutable = new ExpirationMsMutableImpl();
        // Что бы случайно не накосячить в наследовании интерфейсов, сейчас это невозможно, но легко можно сделать обратное
        Assertions.assertFalse(timeControllerMsImmutable instanceof ExpirationMsMutable);
        Assertions.assertFalse(timeControllerMsMutable instanceof ExpirationMsImmutable);

    }

}