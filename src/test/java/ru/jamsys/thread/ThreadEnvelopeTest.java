package ru.jamsys.thread;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.pool.ThreadPool;
import ru.jamsys.rate.limit.v2.RateLimit;
import ru.jamsys.rate.limit.v2.RateLimitName;
import ru.jamsys.util.Util;

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
    public void lastRemove() {
        //Тест того, что удаление ресурсов происходит с конца
        ThreadPool threadPool = new ThreadPool(
                namePool,
                0,
                5,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: false; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.run();
        threadPool.addResourceZeroPool();
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        ThreadEnvelope resource1 = threadPool.getResource();
        Assertions.assertEquals("TestPool-1", resource1.getName());

        Util.sleepMs(1001);
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource2 = threadPool.getResource();
        Assertions.assertEquals("TestPool-2", resource2.getName());

        threadPool.complete(resource1, null);
        threadPool.complete(resource2, null);

        Assertions.assertEquals("resourceQueue: 2; parkQueue: 2; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resourceX = threadPool.getResource();
        Assertions.assertEquals("TestPool-1", resourceX.getName());

        resource2.setKeepAliveOnInactivityMs(1);
        Util.sleepMs(100);
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());
        threadPool.complete(resourceX, null);
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resourceX2 = threadPool.getResource();
        Assertions.assertEquals("TestPool-1", resourceX2.getName());
    }

    @Test
    public void addToRemoveMinMax2() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                5,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        threadPool.run();

        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource1 = threadPool.getResource();

        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive();

        Assertions.assertEquals("resourceQueue: 2; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getResource();

        Assertions.assertEquals("resourceQueue: 2; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        // Проверка, что удалять не из паркинга нельзя
        threadPool.addToRemove(resource1);
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.complete(resource1, null);
        threadPool.addToRemove(resource1);
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 0; removeQueue: 1; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive();
        //  Ожидаем увеличение ресурсов из удалённых, так как в парке нет свободных
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        //Зачистка происходит из парка, а в парке сейчас resource1
        resource1.setKeepAliveOnInactivityMs(1); //По умолчанию сейча 6_000
        Util.sleepMs(100);

        threadPool.keepAlive();
        //Ожидаем срез под нож 1 ресурс
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.shutdown();
    }

    @Test
    public void addToRemoveRemoveMoreMin() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                5,
                5,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        threadPool.run();

        Assertions.assertEquals("resourceQueue: 5; parkQueue: 5; removeQueue: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource = threadPool.getResource();

        Assertions.assertEquals("resourceQueue: 5; parkQueue: 4; removeQueue: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.addToRemove(resource);

        //removeQueue: 0 - так как min = 5
        Assertions.assertEquals("resourceQueue: 5; parkQueue: 4; removeQueue: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.shutdown();
    }

    @Test
    public void checkSetMaxOperation() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                5,
                5,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: false; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.run();

        Assertions.assertEquals("resourceQueue: 5; parkQueue: 5; removeQueue: 0; isRun: true; min: 5; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS).setMax(625);

        for (int i = 0; i < 5; i++) {
            ThreadEnvelope resource = threadPool.getResource();
            resource.run();
        }
        Util.sleepMs(1000);

        Assertions.assertEquals(625, testCount.get());

        threadPool.shutdown();
    }

    @Test
    public void checkAddResource() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                2,
                5,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: false; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.run();
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 2; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope1 = threadPool.getResource();
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 1; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope2 = threadPool.getResource();
        Assertions.assertEquals("resourceQueue: 2; parkQueue: 0; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        Util.sleepMs(1001);
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 3; parkQueue: 1; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.keepAlive();
        //Останется 3 потому что parkQueue > 0, типо незачем наращивать если на парковке есть ресурсы
        Assertions.assertEquals("resourceQueue: 3; parkQueue: 1; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope3 = threadPool.getResource();

        Util.sleepMs(1001);
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 4; parkQueue: 1; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope4 = threadPool.getResource();

        Util.sleepMs(1001);
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 5; parkQueue: 1; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        ThreadEnvelope threadEnvelope5 = threadPool.getResource();
        threadPool.keepAlive();
        //Не должны выйти за 5
        Assertions.assertEquals("resourceQueue: 5; parkQueue: 0; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.keepAlive();
        //Не должны выйти за 5
        Assertions.assertEquals("resourceQueue: 5; parkQueue: 0; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS).setMax(500);

        threadEnvelope1.run();
        threadEnvelope2.run();
        threadEnvelope3.run();
        threadEnvelope4.run();
        threadEnvelope5.run();

        Util.sleepMs(1000);

        Assertions.assertEquals("resourceQueue: 5; parkQueue: 5; removeQueue: 0; isRun: true; min: 2; max: 5; ", threadPool.getMomentumStatistic());

        Assertions.assertEquals(500, testCount.get());

        threadPool.shutdown();
    }

    @Test
    public void checkInitializeThread() {
        testCount.set(0);
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                10,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        threadPool.getRateLimitPoolItem().get(RateLimitName.THREAD_TPS).setMax(100);
        threadPool.run();
        ThreadEnvelope threadEnvelope = threadPool.getResource();

        //Проверяем статусы что мы не инициализированы
        Assertions.assertEquals("isInit: false; isRun: false; isWhile: true; inPark: false; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        // Запускаем поток
        Assertions.assertTrue(threadEnvelope.run());

        //Ждём когда поток поработает
        Util.sleepMs(200);

        // Проверяем что поток ушёл на парковку
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        //Проверяем что отработал предел countOperation
        Assertions.assertEquals(100, testCount.get());

        // Проверяем что поток ушёл на парковку в пуле
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        threadEnvelope = threadPool.getResource();

        //Проверяем что в пуле нет на паркинге никого
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        //Проверяем что отработал вызов polled от pool, который зануляет countOperation
        Assertions.assertEquals("isInit: true; isRun: true; isWhile: true; inPark: true; isShutdown: false; ", threadEnvelope.getMomentumStatistic());

        //Сбросим RateLimit
        threadPool.getRateLimitPoolItem().flushTps(System.currentTimeMillis());
        //Запускаем
        Assertions.assertTrue(threadEnvelope.run());
        //Проверяем, что вышли из паркинга (countOperation удалили вообще =) )
        Assertions.assertTrue(threadEnvelope.getMomentumStatistic().contains("inPark: false;"));

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

        threadPool.shutdown();
    }

    @Test
    public void checkInitialize() { //Проверка что ресурс создаётся при старте pool
        ThreadPool threadPool = new ThreadPool(
                namePool,
                1,
                10,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

        threadPool.run();
        //При инициализации ресурс должен попадать в парковку
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource = threadPool.getResource();
        Assertions.assertNotNull(resource);

        //Так как ресурс взяли - в парковке осталось 0 ресурсов
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 0; removeQueue: 0; isRun: true; min: 1; max: 10; ", threadPool.getMomentumStatistic());

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
                10,
                (ThreadEnvelope threadEnvelope) -> {
                    testCount.incrementAndGet();
                    return true;
                }
        );
        threadPool.getRateLimit().reset();
        threadPool.getRateLimitPoolItem().reset();

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

        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //При старте pool, где min = 0 инициализации ресурсов не должно быть
        Assertions.assertNull(threadPool.getResource());
        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //Если пул min = 1 keepAlive ничего не сделает
        threadPool.keepAlive();
        Assertions.assertEquals("resourceQueue: 0; parkQueue: 0; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        //На помощь приходит старт ресурсов для таких прикольных пулов
        threadPool.addResourceZeroPool();
        Assertions.assertEquals("resourceQueue: 1; parkQueue: 1; removeQueue: 0; isRun: true; min: 0; max: 10; ", threadPool.getMomentumStatistic());

        ThreadEnvelope resource = threadPool.getResource();
        Assertions.assertNotNull(resource);


        //Util.sleepMs(25000);

        threadPool.shutdown();

    }
}