package ru.jamsys.core.thread;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.RateLimitManager;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitType;
import ru.jamsys.core.resource.thread.ThreadEnvelope;
import ru.jamsys.core.resource.thread.ThreadPool;
import ru.jamsys.core.util.Util;


import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class ThreadEnvelopeTest {

    String namePool = "TestPool";
    AtomicInteger testCount = new AtomicInteger(0);

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        if (App.context == null) {
            App.context = SpringApplication.run(App.class, args);
        }
    }



    @Test
    public void lastRemove() {
        //Тест того, что удаление ресурсов происходит с конца
        ThreadPool threadPool = new ThreadPool(
                namePool,
                0,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(5);

        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.run();
        threadPool.addPoolItemIfEmpty();
        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        ThreadEnvelope threadEnvelope1 = threadPool.getPoolItem();
        Assertions.assertEquals("TestPool-1", threadEnvelope1.getName());

        Util.sleepMs(1001);
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope2 = threadPool.getPoolItem();
        Assertions.assertEquals("TestPool-2", threadEnvelope2.getName());

        threadPool.complete(threadEnvelope1, null);
        threadPool.complete(threadEnvelope2, null);

        Assertions.assertEquals("item: 2; park: 2; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelopeX = threadPool.getPoolItem();
        Assertions.assertEquals("TestPool-1", threadEnvelopeX.getName());

        threadEnvelope2.setKeepAliveOnInactivityMs(1);
        Util.sleepMs(100);
        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.complete(threadEnvelopeX, null);
        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelopeX2 = threadPool.getPoolItem();
        Assertions.assertEquals("TestPool-1", threadEnvelopeX2.getName());
    }

    @Test
    public void addToRemoveMinMax2() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(5);

        threadPool.run();

        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope1 = threadPool.getPoolItem();

        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive(null);

        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getPoolItem();

        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        // Проверка, что удалять не из паркинга нельзя
        threadPool.addToRemove(threadEnvelope1);
        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.complete(threadEnvelope1, null);
        threadPool.addToRemove(threadEnvelope1);
        Assertions.assertEquals("item: 2; park: 0; remove: 1; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive(null);
        //  Ожидаем увеличение ресурсов из удалённых, так как в парке нет свободных
        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        //Зачистка происходит из парка, а в парке сейчас te1
        threadEnvelope1.setKeepAliveOnInactivityMs(1); //По умолчанию сейча 6_000
        Util.sleepMs(100);

        threadPool.keepAlive(null);
        //Ожидаем срез под нож 1 ресурс
        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.shutdown();
    }

    @Test
    public void addToRemoveRemoveMoreMin() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                5,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(5);

        threadPool.run();

        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();

        Assertions.assertEquals("item: 5; park: 4; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.addToRemove(threadEnvelope);

        //remove: 0 - так как min = 5
        Assertions.assertEquals("item: 5; park: 4; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.shutdown();
    }

    boolean sleepCondition(int timeOutMs, Supplier<Boolean> supplier) {
        long curTime = System.currentTimeMillis();
        long finishTimeMs = curTime + timeOutMs;
        while (finishTimeMs > System.currentTimeMillis()) {
            if (supplier.get()) {
                System.out.println("Time sleep ms: " + (System.currentTimeMillis() - curTime));
                return true;
            }
            Util.sleepMs(100);
        }
        return false;
    }

    @Test
    public void checkSetMaxOperation() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                5,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(5);

        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.run();

        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getRateLimitPoolItem().get(RateLimitType.THREAD_TPS).setMax(625);

        for (int i = 0; i < 5; i++) {
            threadPool.getPoolItem().run();
        }

        Assertions.assertTrue(sleepCondition(4999, threadPool::allInPark));

        Assertions.assertEquals(625, testCount.get());

        threadPool.shutdown();
    }

    @Test
    public void checkSumCounter() {
        testCount.set(0);
        int countThread = 10;
        ThreadPool threadPool = new ThreadPool(
                namePool,
                countThread,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    Util.sleepMs(10);
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(countThread);

        threadPool.run();

        int c = 1000;

        threadPool.getRateLimitPoolItem().get(RateLimitType.THREAD_TPS).setMax(c);

        for (int i = 0; i < countThread; i++) {
            threadPool.getPoolItem().run();
        }
        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));

        Assertions.assertEquals(c, testCount.get());

        threadPool.shutdown();
    }

    @Test
    public void checkAddThreadEnvelope() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                2,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(5);

        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.run();
        Assertions.assertEquals("item: 2; park: 2; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope1 = threadPool.getPoolItem();
        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope2 = threadPool.getPoolItem();
        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 3; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.keepAlive(null);
        //Останется 3 потому что park > 0, типо незачем наращивать если на парковке есть ресурсы
        Assertions.assertEquals("item: 3; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope3 = threadPool.getPoolItem();

        Util.sleepMs(1001);
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 4; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope4 = threadPool.getPoolItem();

        Util.sleepMs(1001);
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 5; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope5 = threadPool.getPoolItem();
        threadPool.keepAlive(null);
        //Не должны выйти за 5
        Assertions.assertEquals("item: 5; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.keepAlive(null);
        //Не должны выйти за 5
        Assertions.assertEquals("item: 5; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getRateLimitPoolItem().get(RateLimitType.THREAD_TPS).setMax(500);

        threadEnvelope1.run();
        threadEnvelope2.run();
        threadEnvelope3.run();
        threadEnvelope4.run();
        threadEnvelope5.run();

        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));

        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        Assertions.assertEquals(500, testCount.get());

        threadPool.shutdown();
    }

    @Test
    public void checkInitializeThread() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        threadPool.getRateLimitPoolItem().get(RateLimitType.THREAD_TPS).setMax(100);
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(10);
        threadPool.run();
        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();

        //Проверяем статусы что мы не инициализированы
        Assertions.assertEquals("isInit: false; isRun: false; isWhile: true; inPark: false; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        // Запускаем поток
        Assertions.assertTrue(threadEnvelope.run());

        //Ждём когда поток поработает
        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));

        // Проверяем что поток ушёл на парковку
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        //Проверяем что отработал предел countOperation
        Assertions.assertEquals(100, testCount.get());

        // Проверяем что поток ушёл на парковку в пуле
        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        threadEnvelope = threadPool.getPoolItem();

        //Проверяем что в пуле нет на паркинге никого
        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        //Проверяем что отработал вызов polled от pool, который зануляет countOperation
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        //Сбросим RateLimit
        threadPool.getRateLimitPoolItem().flushAndGetStatistic(new HashMap<>(), new HashMap<>(), null);
        //Запускаем
        Assertions.assertTrue(threadEnvelope.run());
        //Проверяем, что вышли из паркинга (countOperation удалили вообще =) )
        Assertions.assertTrue(threadEnvelope.getMomentumStatistic().contains("inPark: false;"));

        //Ждём когда поток поработает
        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));


        //Проверяем что поток реально поработал
        Assertions.assertEquals(200, testCount.get());

        //Дубликаты операций
        Assertions.assertTrue(threadEnvelope.run());
        Assertions.assertFalse(threadEnvelope.run());

        Assertions.assertTrue(threadEnvelope.shutdown());
        Assertions.assertFalse(threadEnvelope.shutdown());

        Assertions.assertFalse(threadEnvelope.run());

        threadPool.shutdown();
    }

    @Test
    public void checkInitialize() { //Проверка что ресурс создаётся при старте pool
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(10);

        threadPool.run();
        //При инициализации ресурс должен попадать в парковку
        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
        Assertions.assertNotNull(threadEnvelope);

        //Так как ресурс взяли - в парковке осталось 0 ресурсов
        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        threadPool.shutdown();
    }

    @Test
    public void checkInitialize2() {

        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
        rateLimitManager.reset(); //Так предыдущие тесты уже насоздавали там данные

        Assertions.assertFalse(rateLimitManager.contains(ThreadPool.class, namePool));
        Assertions.assertFalse(rateLimitManager.contains(ThreadEnvelope.class, namePool));

        ThreadPool threadPool = new ThreadPool(
                namePool,
                0,
                (ThreadEnvelope _) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();
        threadPool.getRateLimit().get(RateLimitType.POOL_SIZE).setMax(10);

        //Проверяем, что RateLimitItem создались в конструкторе ThreadPool
        Assertions.assertTrue(rateLimitManager.contains(ThreadPool.class, namePool));
        Assertions.assertTrue(rateLimitManager.contains(ThreadEnvelope.class, namePool));

        //Проверяем статус новых RateLimitItem - что они не активны, до тех пор пока не стартанёт pool
        RateLimit rateLimitPool = threadPool.getRateLimit();
        Assertions.assertFalse(rateLimitPool.isActive());

        RateLimit rateLimitThread = threadPool.getRateLimit();
        Assertions.assertFalse(rateLimitThread.isActive());

        threadPool.run();

        Assertions.assertTrue(rateLimitPool.isActive());
        Assertions.assertTrue(rateLimitThread.isActive());

        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //При старте pool, где min = 0 инициализации ресурсов не должно быть
        Assertions.assertNull(threadPool.getPoolItem());
        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //Если пул min = 1 keepAlive ничего не сделает
        threadPool.keepAlive(null);
        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //На помощь приходит старт ресурсов для таких прикольных пулов
        threadPool.addPoolItemIfEmpty();
        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
        Assertions.assertNotNull(threadEnvelope);


        //Util.sleepMs(25000);

        threadPool.shutdown();

    }
}