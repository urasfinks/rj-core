package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.flat.util.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadEnvelopeTest {

    String namePool = "TestPool";
    AtomicInteger testCount = new AtomicInteger(0);

    @BeforeAll
    static void beforeAll() {
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        if (App.context == null) {
            App.context = SpringApplication.run(App.class);
            App.context.getBean(ServiceProperty.class).setProperty("run.args.remote.log", "false");
        }
    }

    @AfterAll
    static void shutdown() {
        App.context = null;
    }

//    @Test
//    public void lastRemove() {
//        //Тест того, что удаление ресурсов происходит с конца
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                0,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(5);
//
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 0; max: 5; ", threadPool.getMomentumStatistic());
//        threadPool.run();
//        threadPool.addIfPoolEmpty();
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
//        ThreadEnvelope threadEnvelope1 = threadPool.getPoolItem();
//        Assertions.assertEquals("TestPool-1", threadEnvelope1.getName());
//
//        Util.sleepMs(1001);
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope2 = threadPool.getPoolItem();
//        Assertions.assertEquals("TestPool-2", threadEnvelope2.getName());
//
//        threadPool.complete(threadEnvelope1, null);
//        threadPool.complete(threadEnvelope2, null);
//
//        Assertions.assertEquals("item: 2; park: 2; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelopeX = threadPool.getPoolItem();
//        Assertions.assertEquals("TestPool-1", threadEnvelopeX.getName());
//
//        threadEnvelope2.setKeepAliveOnInactivityMs(1);
//        Util.sleepMs(100);
//        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
//
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void addToRemoveMinMax2() {
//        testCount.set(0);
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                1,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(5);
//
//        threadPool.run();
//
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope r1 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.complete(r1, null);
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        r1 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        Util.sleepMs(1001);
//        threadPool.keepAlive(null);
//        ThreadEnvelope r2 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.complete(r1, null);
//        threadPool.complete(r2, null);
//        Assertions.assertEquals("item: 2; park: 2; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        r1 = threadPool.getPoolItem();
//        r2 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.complete(r1, null);
//        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        r1.setLastActivityMs(System.currentTimeMillis() - 100000);
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 2; park: 0; remove: 1; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.complete(r1, null);
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.complete(r2, null);
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void addToRemoveRemoveMoreMin() {
//        testCount.set(0);
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                5,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(5);
//
//        threadPool.run();
//
//        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
//
//        Assertions.assertEquals("item: 5; park: 4; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());
//
////        threadPool.addToRemoveOnlyTest(threadEnvelope);
////
////        //remove: 0 - так как min = 5
////        Assertions.assertEquals("item: 5; park: 4; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.shutdown();
//    }
//
//    boolean sleepCondition(int timeOutMs, Supplier<Boolean> supplier) {
//        long curTime = System.currentTimeMillis();
//        long finishTimeMs = curTime + timeOutMs;
//        while (finishTimeMs > System.currentTimeMillis()) {
//            if (supplier.get()) {
//                System.out.println("Time sleep ms: " + (System.currentTimeMillis() - curTime));
//                return true;
//            }
//            Util.sleepMs(100);
//        }
//        return false;
//    }
//
//    @Test
//    public void checkSetMaxOperation() {
//        testCount.set(0);
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                5,
//                (ThreadEnvelope _) -> {
//                    //System.out.println(testCount.get());
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(5);
//
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 5; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.run();
//
//        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS.getName()).setMax(625);
//
//        for (int i = 0; i < 5; i++) {
//            threadPool.getPoolItem().run();
//        }
//
//        Assertions.assertTrue(sleepCondition(4999, threadPool::allInPark));
//
//        Assertions.assertEquals(625, testCount.get());
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void checkSumCounter() {
//        testCount.set(0);
//        int countThread = 10;
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                countThread,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    Util.sleepMs(10);
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(countThread);
//
//        threadPool.run();
//
//        int c = 1000;
//
//        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS.getName()).setMax(c);
//
//        for (int i = 0; i < countThread; i++) {
//            threadPool.getPoolItem().run();
//        }
//        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));
//
//        Assertions.assertEquals(c, testCount.get());
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void checkAddThreadEnvelope() {
//        testCount.set(0);
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                2,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(5);
//
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: false; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.run();
//        Assertions.assertEquals("item: 2; park: 2; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope1 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 2; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope2 = threadPool.getPoolItem();
//        Assertions.assertEquals("item: 2; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        Util.sleepMs(1001);
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 3; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.keepAlive(null);
//        //Останется 3 потому что park > 0, типо незачем наращивать если на парковке есть ресурсы
//        Assertions.assertEquals("item: 3; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope3 = threadPool.getPoolItem();
//
//        Util.sleepMs(1001);
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 4; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope4 = threadPool.getPoolItem();
//
//        Util.sleepMs(1001);
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 5; park: 1; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope5 = threadPool.getPoolItem();
//        threadPool.keepAlive(null);
//        //Не должны выйти за 5
//        Assertions.assertEquals("item: 5; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.keepAlive(null);
//        //Не должны выйти за 5
//        Assertions.assertEquals("item: 5; park: 0; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS.getName()).setMax(500);
//
//        threadEnvelope1.run();
//        threadEnvelope2.run();
//        threadEnvelope3.run();
//        threadEnvelope4.run();
//        threadEnvelope5.run();
//
//        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));
//
//        Assertions.assertEquals("item: 5; park: 5; remove: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());
//
//        Assertions.assertEquals(500, testCount.get());
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void checkInitializeThread() {
//        testCount.set(0);
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                1,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//
//        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS.getName()).setMax(100);
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(10);
//        threadPool.run();
//        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
//
//        //Проверяем статусы что мы не инициализированы
//        Assertions.assertEquals("isInit: false; isRun: false; isWhile: true; inPark: false; isShutdown: false; ", threadEnvelope.getMomentumStatistic());
//
//        // Запускаем поток
//        Assertions.assertTrue(threadEnvelope.run());
//
//        //Ждём когда поток поработает
//        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));
//
//        // Проверяем что поток ушёл на парковку
//        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());
//
//        //Проверяем что отработал предел countOperation
//        Assertions.assertEquals(100, testCount.get());
//
//        // Проверяем что поток ушёл на парковку в пуле
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());
//
//        threadEnvelope = threadPool.getPoolItem();
//
//        //Проверяем что в пуле нет на паркинге никого
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());
//
//        //Проверяем что отработал вызов polled от pool, который зануляет countOperation
//        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());
//
//        //Сбросим RateLimit
//        threadPool.getRateLimitPoolItem().flushAndGetStatistic(new HashMap<>(), new HashMap<>(), null);
//        //Запускаем
//        Assertions.assertTrue(threadEnvelope.run());
//        //Проверяем, что вышли из паркинга (countOperation удалили вообще =) )
//        // Не конкурентная проверка
//        Assertions.assertTrue(threadEnvelope.getMomentumStatistic().contains("inPark: false;"));
//
//        //Ждём когда поток поработает
//        Assertions.assertTrue(sleepCondition(5000, threadPool::allInPark));
//
//
//        //Проверяем что поток реально поработал
//        Assertions.assertEquals(200, testCount.get());
//
//        //Дубликаты операций
//        Assertions.assertTrue(threadEnvelope.run());
//        Assertions.assertFalse(threadEnvelope.run());
//
//        Assertions.assertTrue(threadEnvelope.shutdown());
//        Assertions.assertFalse(threadEnvelope.shutdown());
//
//        Assertions.assertFalse(threadEnvelope.run());
//
//        threadPool.shutdown();
//    }
//
//    @Test
//    public void checkInitialize() { //Проверка что ресурс создаётся при старте pool
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                1,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(10);
//
//        threadPool.run();
//        //При инициализации ресурс должен попадать в парковку
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
//        Assertions.assertNotNull(threadEnvelope);
//
//        //Так как ресурс взяли - в парковке осталось 0 ресурсов
//        Assertions.assertEquals("item: 1; park: 0; remove: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());
//
//        threadPool.shutdown();
//    }
//
//    @SuppressWarnings("all")
//    @Test
//    public void checkInitialize2() {
//        //Так предыдущие тесты уже насоздавали там данные
//        App.context = SpringApplication.run(App.class);
//
//        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
//
//        Assertions.assertFalse(rateLimitManager.getTestMap().containsKey(ClassNameImpl.getClassNameStatic(ThreadPool.class, namePool)));
//        Assertions.assertFalse(rateLimitManager.getTestMap().containsKey(ClassNameImpl.getClassNameStatic(ThreadEnvelope.class, namePool)));
//
//        ThreadPool threadPool = new ThreadPool(
//                namePool,
//                0,
//                (ThreadEnvelope _) -> {
//                    testCount.incrementAndGet();
//                    return true;
//                }
//        );
//        threadPool.getRateLimit().reset();
//        threadPool.getRateLimitPoolItem().reset();
//        threadPool.getRateLimit().get(RateLimitName.POOL_SIZE.getName()).setMax(10);
//
//        //Проверяем, что RateLimitItem создались в конструкторе ThreadPool
//        Assertions.assertTrue(rateLimitManager.getTestMap().containsKey(threadPool.getClassName(namePool)));
//        Assertions.assertTrue(rateLimitManager.getTestMap().containsKey(threadPool.getClassName(namePool)));
//
//        //Проверяем статус новых RateLimitItem - что они не активны, до тех пор пока не стартанёт pool
//        RateLimit rateLimitPool = threadPool.getRateLimit();
//        Assertions.assertFalse(rateLimitPool.isActive());
//
//        RateLimit rateLimitThread = threadPool.getRateLimit();
//        Assertions.assertFalse(rateLimitThread.isActive());
//
//        threadPool.run();
//
//        Assertions.assertTrue(rateLimitPool.isActive());
//        Assertions.assertTrue(rateLimitThread.isActive());
//
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());
//
//        //При старте pool, где min = 0 инициализации ресурсов не должно быть
//        Assertions.assertNull(threadPool.getPoolItem());
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());
//
//        //Если пул min = 1 keepAlive ничего не сделает
//        threadPool.keepAlive(null);
//        Assertions.assertEquals("item: 0; park: 0; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());
//
//        //На помощь приходит старт ресурсов для таких прикольных пулов
//        threadPool.addIfPoolEmpty();
//        Assertions.assertEquals("item: 1; park: 1; remove: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());
//
//        ThreadEnvelope threadEnvelope = threadPool.getPoolItem();
//        Assertions.assertNotNull(threadEnvelope);
//
//
//        //Util.sleepMs(25000);
//
//        threadPool.shutdown();
//
//    }

    @Test
    void test() {
        ConcurrentHashMap<Long, AvgMetric> map = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while (map.size() < 3) {
                    long sec = Util.zeroLastNDigits(System.currentTimeMillis(), 3);
                    map.computeIfAbsent(sec, _ -> {
                        counter.incrementAndGet();
                        return new AvgMetric();
                    });
                    Thread.onSpinWait();
                }

            }).start();
        }
        Util.sleepMs(4000);
        System.out.println("map.size() = " + map.size() + "; counter: " + counter.get());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals(3, counter.get());
    }



    @Test
    void test3() {
        ConcurrentHashMap<Long, AvgMetric> map = new ConcurrentHashMap<>();
        AvgMetric avgMetric = map.computeIfAbsent(1L, _ -> new AvgMetric());
        AvgMetric avgMetric2 = map.computeIfAbsent(1L, _ -> new AvgMetric());
        Assertions.assertEquals(avgMetric, avgMetric2);
    }

}