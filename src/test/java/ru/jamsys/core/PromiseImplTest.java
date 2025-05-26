package ru.jamsys.core;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.log.LogType;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.*;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.tps.RateLimitTps;
import ru.jamsys.core.resource.http.HttpResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 16.956 (17885)
// COMPUTE time: 15.124 (24859)

class PromiseImplTest {

    public static ServicePromise servicePromise;

    static long start;

    @BeforeAll
    static void beforeAll() {
        start = System.currentTimeMillis();
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
        UtilLog.printInfo("Test time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void test01() {
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        AtomicInteger x = new AtomicInteger(0);
        promise
                .append("test", (_, _, _) -> x.incrementAndGet())
                .run()
                .await(4000);
        Assertions.assertEquals(1, x.get());
    }

    @Test
    void test02() {
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        AtomicInteger x = new AtomicInteger(0);
        promise
                .append("test1", (_, _, _) -> x.incrementAndGet())
                .append("test2", (_, _, _) -> x.incrementAndGet())
                .run()
                .await(4000);
        Assertions.assertEquals(2, x.get());
    }

    @Test
    void test03() {
        // Это проверка, что первый then генерирует 0 элемент в очереди wait, логика должна пропустить его
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        StringBuilder sb = new StringBuilder();
        promise
                .then("test1", (_, _, _) -> sb.append("1"))
                .then("test2", (_, _, _) -> sb.append("2"))
                .then("test3", (_, _, _) -> sb.append("3"))
                .run()
                .await(4000);
        Assertions.assertEquals("123", sb.toString());
    }

    @Test
    void test04() {
        // Тут тестируем последний элемент wait, который не должен привести к бесконечным ожиданиям
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        StringBuilder sb = new StringBuilder();
        promise
                .then("test1", (_, _, _) -> sb.append("1"))
                .then("test2", (_, _, _) -> sb.append("2"))
                .then("test3", (_, _, _) -> sb.append("3"))
                .appendWait("mey")
                .run()
                .await(4000);
        Assertions.assertEquals("123", sb.toString());
        Assertions.assertEquals(Promise.TerminalStatus.SUCCESS, promise.getTerminalStatus());
    }

    @Test
    void test1() {
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        promise
                .append("test", (_, _, promise1) -> {
                    Util.testSleepMs(1000);
                    ArrayList<AbstractPromiseTask> objects = new ArrayList<>();
                    objects.add(new PromiseTask("test2", promise, PromiseTaskExecuteType.COMPUTE, (_, _, _) -> UtilLog.printInfo("EXTRA")));
                    promise1.getQueueTask().addFirst(objects);
                })
                .append("test", (_, _, _) -> Util.testSleepMs(1000))
                .then("test", (_, _, _) -> Util.testSleepMs(1000))
                .run()
                .await(4000);
    }

    @Test
    void test2() {
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            promise.then("test", (_, _, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(500);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3() {
        ManagerConfiguration<RateLimit> rateLimitItemConfiguration = ManagerConfiguration.getInstance(
                RateLimitTps.class,
                Promise.getComplexIndex("test", "test")
        );
        rateLimitItemConfiguration.get().setMax(10000);
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();

        for (int i = 0; i < 1000; i++) {
            final int x = i;
            promise.then("test", (_, _, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(1100);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3_1() {
        Promise promise = servicePromise.get("seq", 6_000L);
        AtomicInteger c = new AtomicInteger(0);
        promise.then("then1", (_, _, _) -> c.incrementAndGet());

        ManagerConfiguration<RateLimit> rateLimitItemConfiguration = promise
                .getQueueTask()
                .get("seq.then1")
                .getComputeThreadConfiguration().get()
                .getRateLimitConfiguration();
        Assertions.assertEquals(999999, rateLimitItemConfiguration.get().getMax());
        rateLimitItemConfiguration.get().setMax(1);
        Assertions.assertEquals(1, rateLimitItemConfiguration.get().getMax());

        promise.then("then1", (_, _, _) -> c.incrementAndGet());

        promise.run().await(1000);
        // Для IO потоков нет ограничений по tps, поэтому там будет expected = 2 это нормально!
        Assertions.assertEquals(1, c.get());
    }

    @Test
    void test3_2() {
        AtomicInteger c = new AtomicInteger(0);
        Promise promise = servicePromise.get("seq2", 1_500L)
                .then("then1", (_, _, _) -> c.incrementAndGet())
                .modifyLastPromiseTask(abstractPromiseTask -> {
                    // --
                    abstractPromiseTask
                            .getComputeThreadConfiguration().get()
                            .getRateLimitConfiguration().get()
                            .setMax(0);
                })
                .run().
                await(3000);
        // Для IO потоков нет ограничений по tps, поэтому там будет expected = 2 это нормально!
        Assertions.assertEquals(Promise.TerminalStatus.ERROR, promise.getTerminalStatus());
        Assertions.assertEquals(0, c.get());
    }


    @Test
    void test4() {
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            promise.then("test", (_, _, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(3000);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test5() {
        ManagerConfiguration<RateLimit> rateLimitItemConfiguration = ManagerConfiguration.getInstance(
                RateLimitTps.class,
                Promise.getComplexIndex("test", "test")
        );
        rateLimitItemConfiguration.get().setMax(100000000);
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 1000; i++) {
            final int x = i;
            promise.then("test", (_, _, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run();
        promise.await(5000);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test6() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            test5();
            Thread.onSpinWait();
        }
        UtilLog.printInfo("sum time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void test7() {
        Promise promise = servicePromise.get("test", 6_000L);
        AtomicInteger retry = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        promise
                .append("test", (_, _, _) -> {
                    retry.incrementAndGet();
                    throw new RuntimeException("Hello world");
                })
                .modifyLastPromiseTask(abstractPromiseTask -> {
                    abstractPromiseTask.setRetryCount(1);
                    abstractPromiseTask.setRetryDelayMs(1000);
                })
                .onError((_, _, _) -> error.incrementAndGet())
                .onComplete((_, _, _) -> complete.incrementAndGet())
                .run()
                .await(3000);
        Assertions.assertEquals(2, retry.get());
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
    }

    @Test
    void testTimeOut() {
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        AtomicInteger exec = new AtomicInteger(0);

        Promise promise = servicePromise.get("test", 1_500L);
        promise
                .append("1", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1000);
                })
                .then("2", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1000);
                })
                .then("3", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1000);
                })
                .onError((_, _, _) -> error.incrementAndGet())
                .onComplete((_, _, _) -> complete.incrementAndGet())
                .run()
                .await(3000);
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
        Assertions.assertEquals(2, exec.get());
    }

    @Test
    void testTimeOutParallel() {
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        AtomicInteger exec = new AtomicInteger(0);

        Promise promise = servicePromise.get("test", 1_500L);
        promise
                .append("1", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1000);
                })
                .append("2", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1500);
                })
                .then("3", (_, _, _) -> {
                    exec.incrementAndGet();
                    Util.testSleepMs(1000);
                })
                .onError((_, _, _) -> error.incrementAndGet())
                .onComplete((_, _, _) -> complete.incrementAndGet())
                .run()
                .await(2000);
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
        Assertions.assertEquals(2, exec.get());
    }

    @Test
    void testOneTaskExecutionTime() {
        AtomicInteger x = new AtomicInteger(0);
        Promise promise = servicePromise.get("testOneTaskExecutionTime", 1_500L);
        promise
                .append("1", (_, _, _) -> {

                })
                .onComplete((_, _, _) -> x.incrementAndGet())
                .run()
                .await(3000);
        Assertions.assertEquals(1, x.get());
    }

    @Test
    void testNoTask() {
        // Выполнение onComplete если нет задач у обещания
        AtomicInteger x = new AtomicInteger(0);
        Promise promise = servicePromise.get("test", 1_500L);
        promise
                .onComplete((_, _, _) -> x.incrementAndGet())
                .run()
                .await(1000);
        Assertions.assertEquals(1, x.get());
    }

    @Test
    void testExternalWait() {
        Promise promise = servicePromise.get("Async", 6_000L);
        AbstractPromiseTask externalPromiseTask = new PromiseTask(
                "test",
                promise,
                PromiseTaskExecuteType.ASYNC_COMPUTE,
                null
        );
        promise.append(externalPromiseTask);
        promise.run().await(1000);

        Assertions.assertEquals(2, promise.getTrace().size()); // Так как добавился Run
        Assertions.assertTrue(promise.isRun());

        externalPromiseTask.getPromise().completePromiseTask(externalPromiseTask);
        Assertions.assertFalse(promise.isRun());
        UtilLog.printInfo(promise);
        Assertions.assertEquals(3, promise.getTrace().size());

    }

    @Test
    void testExpiration() {
        Promise promise = servicePromise.get("Expiration", 1_000L);
        AtomicInteger counter = new AtomicInteger(0);
        promise
                .append("longTimeout", (_, _, _)
                        -> Util.testSleepMs(2000))
                .onError((_, _, _) -> counter.incrementAndGet())
                .run()
                .await(2010);

        Assertions.assertFalse(promise.isRun());
        Assertions.assertEquals(Promise.TerminalStatus.ERROR, promise.getTerminalStatus());
        Assertions.assertEquals(1, counter.get());
        UtilLog.printInfo(promise);
    }

    @Test
    void waitBeforeExternalTask() {
        Promise promise = servicePromise.get("testPromise", 1_000L);
        promise
                .append("st", (_, _, promise1) -> {
                    AbstractPromiseTask asyncPromiseTask = new PromiseTask(
                            "async",
                            promise1,
                            PromiseTaskExecuteType.ASYNC_IO,
                            null
                    );
                    List<AbstractPromiseTask> add = new ArrayList<>();
                    add.add(asyncPromiseTask);
                    add.add(new PromiseTaskWait(promise1));
                    promise1.getQueueTask().addFirst(add);
                })
                .run()
                .await(2000);

        //Мы по timeout должны упасть
        Assertions.assertEquals(Promise.TerminalStatus.ERROR, promise.getTerminalStatus());
        // Мы дали 1 секунду время жизни, EXTERNAL_WAIT не финишировал -> сработал timeout
        Assertions.assertFalse(promise.isRun());
        UtilLog.printInfo(promise);

    }

    @Test
    void testSkipUntil() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise
                .then("1task", (_, promiseTask1, promise1) -> promise1.skipUntil(promiseTask1, "task3"))
                .then("task2", (_, _, _) -> xx.incrementAndGet())
                .then("task3", (_, _, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        Assertions.assertEquals(1, xx.get());
    }

    @Test
    void testSkipUntil2() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise
                .then("1task", (_, promiseTask1, promise1) -> promise1.skipUntil(promiseTask1, "task5"))
                .then("task2", (_, _, _) -> xx.incrementAndGet())
                .then("task3", (_, _, _) -> xx.incrementAndGet())
                .then("task4", (_, _, _) -> xx.incrementAndGet())
                .then("task5", (_, _, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        Assertions.assertEquals(1, xx.get());
    }

    @Test
    void testSkipUntilError() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise.then("1task", (_, promiseTask1, promise1) -> {
                    // --
                    promise1.skipUntil(promiseTask1, "task6");
                })
                .then("task2", (_, _, _) -> xx.incrementAndGet())
                .then("task3", (_, _, _) -> xx.incrementAndGet())
                .then("task4", (_, _, _) -> xx.incrementAndGet())
                .then("task5", (_, _, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        Assertions.assertEquals(0, xx.get());
        Assertions.assertEquals(Promise.TerminalStatus.ERROR, promise.getTerminalStatus());
        UtilLog.printInfo(promise);
    }

    @Test
    void testAppendWait() {
        Promise promise = servicePromise.get("log", 6_000L);
        promise.setLogType(LogType.DEBUG);
        promise.appendWait();
        Assertions.assertEquals("[]", promise.getQueueTask().getMainQueue().toString());
        promise.then("index", (_, _, _) -> {
        });
        Assertions.assertEquals("[AbstractPromiseTask(type=COMPUTE, ns=log.index)]", promise.getQueueTask().getMainQueue().toString());
        promise.appendWait();
        Assertions.assertEquals("[AbstractPromiseTask(type=COMPUTE, ns=log.index), PromiseTaskWait()]", promise.getQueueTask().getMainQueue().toString());
        promise.appendWait();
        Assertions.assertEquals("[AbstractPromiseTask(type=COMPUTE, ns=log.index), PromiseTaskWait()]", promise.getQueueTask().getMainQueue().toString());
    }

    @Test
    void testLog() {
        Promise promise = servicePromise.get("log", 6_000L);
        promise.setLogType(LogType.DEBUG);
        promise.extension(promise1 -> promise1.setRepositoryMap("x", ""));
        promise.extension(promise1 -> promise1.setRepositoryMap("y", "z"));
        promise.extension(promise1 -> promise1.setRepositoryMapClass(X.class, new X()));
        promise
                .thenWithResource("http", HttpResource.class, (_, _, _, _) -> {
                })
                .then("1task", (_, _, promise1) -> {
                    X x = promise1.getRepositoryMapClass(X.class);
                    x.setValue("Hello world");
                })
                .then("2task", (_, _, promise1) -> {
                    X x = promise1.getRepositoryMapClass(X.class);
                    Assertions.assertEquals("Hello world", x.getValue());
                })
                .run()
                .await(1000)
        ;
        UtilLog.printInfo(promise);
    }

    @Getter
    @Setter
    public static class X {
        private String value;
    }

    @Test
    void testSkipUntilAndSkippAll() {
        Promise promise = servicePromise.get("log", 6_000L);
        promise.setLogType(LogType.DEBUG);
        AtomicInteger onError = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);

        promise.then("index", (_, promiseTask1, promise1) -> {
                    promise1.skipUntil(promiseTask1, "NotExist");
                    promise1.skipAllStep(promiseTask1,"system");
                })
                .then("index2", (_, _, _) -> {

                })
                .onError((_, _, _) -> {
                    onError.incrementAndGet();
                    throw new RuntimeException("OPA BUG");
                })
                .onComplete((_, _, _) -> onComplete.incrementAndGet())
                .run().await(7000);
        UtilLog.printInfo(promise);
        Assertions.assertEquals(1, onError.get());
        Assertions.assertEquals(0, onComplete.get());
    }

    @Test
    void testThenPromise() {
        Promise promise = servicePromise.get("testThenPromise", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        List<Integer> list = new ArrayList<>();
        promise.then("index0", (_, _, _) -> list.add(0))
                .then("index1", servicePromise.get("log2", 6_000L)
                        .then("sub1", (_, _, _) -> {
                            Util.testSleepMs(300);
                            list.add(1);
                        })
                        .then("sub1", (_, _, _) -> list.add(2))
                )
                .then("index2", (_, _, _) -> list.add(3))
                .run()
                .await(7000);
        Assertions.assertEquals("[0, 1, 2, 3]", list.toString());
    }

    @Test
    void testThenPromise2() {
        Promise promise = servicePromise.get("testThenPromise2", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        List<Integer> list = new ArrayList<>();
        promise.then("index0", (_, _, _) -> list.add(0))
                .append("index1", servicePromise.get("log2", 6_000L)
                        .then("sub1", (_, _, _) -> {
                            Util.testSleepMs(300);
                            list.add(1);
                        })
                        .then("sub1", (_, _, _) -> list.add(2))
                )
                .append("index2", (_, _, _) -> list.add(3))
                .then("index3", (_, _, _) -> list.add(4))
                .run().await(7000);
        Assertions.assertEquals("[0, 3, 1, 2, 4]", list.toString());
    }

    @Test
    void testThenPromise3() {
        Promise promise = servicePromise.get("testThenPromise3", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        AtomicInteger x = new AtomicInteger(0);
        promise
                .then("subPromise", servicePromise.get("log2", 6_000L)
                        .then("sub1", (_, _, promise1)
                                -> promise1.setRepositoryMap("hello", "world"))
                )
                .then("getResultSubPromise", (_, _, promise1) -> {
                    Promise subPromise = promise1.getRepositoryMapClass(Promise.class, "subPromise");
                    String hello = subPromise.getRepositoryMap(String.class, "hello");
                    if (hello.equals("world")) {
                        x.incrementAndGet();
                    }
                })
                .run().await(7000);
        Assertions.assertEquals(1, x.get());
    }

    @Test
    void testPromise() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        AtomicInteger x = new AtomicInteger(0);
        promise
                .then("i1", (_, _, _) -> x.incrementAndGet())
                .then("i2", (_, _, _) -> x.incrementAndGet())
                .then("i3", (_, _, _) -> x.incrementAndGet());

        new PromiseTest(promise)
                .remove("i1")
                .replace("i2", promise.createTaskCompute("i2_2", (_, _, _) -> x.addAndGet(4)));
        promise.run().await(7000);
        Assertions.assertEquals(5, x.get());
    }

    @Test
    void testPromiseAfter() {
        Promise promise = servicePromise.get("testPromiseAfter", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        AtomicInteger x = new AtomicInteger(0);
        promise
                .then("i1", (_, _, _) -> x.incrementAndGet())
                .then("i2", (_, _, _) -> x.incrementAndGet())
                .then("i3", (_, _, _) -> x.incrementAndGet())
                .then("i4", (_, _, _) -> x.incrementAndGet())
                .then("i5", (_, _, _) -> x.incrementAndGet())
                .then("i6", (_, _, _) -> x.incrementAndGet())
        ;

        PromiseTest promiseTest = new PromiseTest(promise);

        Assertions.assertEquals("[i3::WAIT, i3::COMPUTE, i4::WAIT, i4::COMPUTE, i5::WAIT, i5::COMPUTE, i6::WAIT, i6::COMPUTE]", promiseTest.removeBefore("i3").getIndex().toString());
        Assertions.assertEquals("[i3::WAIT, i3::COMPUTE, i4::WAIT, i4::COMPUTE, i5::WAIT, i5::COMPUTE]", promiseTest.removeAfter("i5").getIndex().toString());
    }

    @Test
    void testPromiseExternal() {
        Promise promise = servicePromise.get("testPromiseExternal", 6_000L);
        //promise.setLogType(LogType.DEBUG);
        promise
                .then("i1", (_, _, _) -> {
                })
                .then("subPromise", servicePromise.get("log", 6_000L))
        ;

        PromiseTest promiseTest = new PromiseTest(promise);

        Assertions.assertEquals("[i1::COMPUTE, subPromise::WAIT, subPromise::ASYNC_COMPUTE]", promiseTest.getIndex().toString());
    }

}