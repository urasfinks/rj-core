package ru.jamsys.thread;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.RateLimit;
import ru.jamsys.statistic.RateLimitGroup;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadEnvelopeTest {

    String namePool = "TestPool";
    AtomicInteger testCount = new AtomicInteger(0);

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    public void checkInitializeThread() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                10,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.run();
        ThreadEnvelope threadEnvelope = threadPool.getResource();

        //Проверяем статусы что мы не инициализированы
        Assertions.assertEquals("isInit: false; isRun: false; isWhile: true; inPark: false; isShutdown: false; countOperation: 0; ", threadEnvelope.getMomentumStatistic());

        // Запускаем поток
        Assertions.assertTrue(threadEnvelope.run());

        //Проверяем что поменялись статусы инициализации
        String momentumStatistic = threadEnvelope.getMomentumStatistic();
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: false; isShutdown: false; ", momentumStatistic.substring(0, momentumStatistic.indexOf("countOperation:")));

        //Ждём когда поток поработает
        Util.sleepMs(200);

        // Проверяем что поток ушёл на парковку
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; countOperation: 101; ", threadEnvelope.getMomentumStatistic());

        //Проверяем что отработал предел countOperation
        Assertions.assertEquals(100, testCount.get());

        // Проверяем что поток ушёл на парковку в пуле
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        threadEnvelope = threadPool.getResource();

        //Проверяем что в пуле нет на паркинге никого
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        //Проверяем что отработал вызов polled от pool, который зануляет countOperation
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; countOperation: 0; ", threadEnvelope.getMomentumStatistic());

        //Запускаем
        Assertions.assertTrue(threadEnvelope.run());
        //Проверяем, что вышли из паркинга
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: false; isShutdown: false; countOperation: 0; ", threadEnvelope.getMomentumStatistic());

        //Ждём когда поток поработает
        Util.sleepMs(200);

        //Проверяем что поток реально поработал
        Assertions.assertEquals(200, testCount.get());

        //Дубликаты операций
        Assertions.assertTrue(threadEnvelope.run());
        Assertions.assertFalse(threadEnvelope.run());

        Assertions.assertTrue(threadEnvelope.shutdown());
        Assertions.assertFalse(threadEnvelope.shutdown());

        Assertions.assertFalse(threadEnvelope.run());
    }

    @Test
    public void checkInitialize() { //Проверка что ресурс создаётся при старте pool
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                10,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.run();
        //При инициализации ресурс должен попадать в парковку
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource = threadPool.getResource();
        Assertions.assertNotNull(resource);

        //Так как ресурс взяли - в парковке осталось 0 ресурсов
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());
    }

    @Test
    public void checkInitialize2() {

        RateLimit rateLimit = App.context.getBean(RateLimit.class);


        Assertions.assertFalse(rateLimit.contains(RateLimitGroup.POOL, ThreadPool.class, namePool));
        Assertions.assertFalse(rateLimit.contains(RateLimitGroup.THREAD, ThreadPool.class, namePool));

        ThreadPool threadPool = new ThreadPool(
                namePool,
                0,
                10,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );

        //Проверяем, что RateLimitItem создались в конструкторе ThreadPool
        Assertions.assertTrue(rateLimit.contains(RateLimitGroup.POOL, ThreadPool.class, namePool));
        Assertions.assertTrue(rateLimit.contains(RateLimitGroup.THREAD, ThreadPool.class, namePool));

        //Проверяем статус новых RateLimitItem - что они не активны, до тех пор пока не стартанёт pool
        RateLimitItem rateLimitItemPool = threadPool.getRateLimitItemPool();
        Assertions.assertFalse(rateLimitItemPool.isActive());

        RateLimitItem rateLimitItemThread = threadPool.getRateLimitItemThread();
        Assertions.assertFalse(rateLimitItemThread.isActive());

        threadPool.run();

        Assertions.assertTrue(rateLimitItemPool.isActive());
        Assertions.assertTrue(rateLimitItemThread.isActive());

        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //При старте pool, где min = 0 инициализации ресурсов не должно быть
        Assertions.assertNull(threadPool.getResource());
        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());


        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource = threadPool.getResource();
        Assertions.assertNotNull(resource);


        //Util.sleepMs(25000);

    }
}