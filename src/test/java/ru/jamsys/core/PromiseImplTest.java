package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.jdbc.JdbcResource;

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
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    void test1() {
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        promise
                .append("test", (_, promise1) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H1");
                    ArrayList<PromiseTask> objects = new ArrayList<>();
                    objects.add(new PromiseTask("test2", promise, App.getUsualExecutor(), (_, _) -> System.out.println(Thread.currentThread().getName() + " EXTRA")));
                    promise1.addToHead(objects);
                })
                .append("test", (_, _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H2");

                })
                .then("test", (_, _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H3");
                })
                .append("test", (_, _) -> System.out.println(Thread.currentThread().getName() + " FINISH"))
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
            promise.then("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(500);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
        App.error(new RuntimeException("Hello"));
    }

    @Test
    void test3() {
        App.get(ManagerRateLimit.class).setLimit("ThreadPool.test.test", "tps", 10000);
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();

        for (int i = 0; i < 1000; i++) {
            final int x = i;
            promise.then("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(1100);
        System.out.println("test3 isRun: " + promise.isRun());
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3_1() {
        App.get(ManagerRateLimit.class).setLimit("ThreadPool.seq.then1", "tps", 1);
        Promise promise = servicePromise.get("seq", 6_000L);
        AtomicInteger c = new AtomicInteger(0);
        promise.then("then1", (_, _) -> c.incrementAndGet());
        promise.then("then1", (_, _) -> c.incrementAndGet());

        promise.run().await(1000);
        System.out.println(promise.getLogString());
        // Для IO потоков нет ограничений по tps, поэтому там будет expected = 2 это нормально!
        Assertions.assertEquals(1, c.get());
    }

    @Test
    void test4() {
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            promise.then("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(3000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test5() {
        App.get(ManagerRateLimit.class).setLimit("ThreadPool.test.test", "tps", 100000000);
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 1000; i++) {
            final int x = i;
            promise.then("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        //System.out.println("start size: " + wf.getListPendingTasks().size());
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
        System.out.println("sum time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void test7() {
        Promise promise = servicePromise.get("test", 6_000L);
        AtomicInteger retry = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        promise
                .append("test", (_, _) -> {
                    retry.incrementAndGet();
                    throw new RuntimeException("Hello world");
                })
                .getLastTask().setRetryCount(1, 1000).getPromise()
                .onError((_, _) -> error.incrementAndGet())
                .onComplete((_, _) -> complete.incrementAndGet())
                .run()
                .await(3000);
        System.out.println(promise.getLogString());
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
                .append("1", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("2", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("3", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError((_, _) -> error.incrementAndGet())
                .onComplete((_, _) -> complete.incrementAndGet())
                .run()
                .await(2100);
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
                .append("1", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .append("2", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1500);
                })
                .then("3", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError((_, _) -> error.incrementAndGet())
                .onComplete((_, _) -> complete.incrementAndGet())
                .run()
                .await(2000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
        Assertions.assertEquals(2, exec.get());
    }

    @Test
    void toLog() {
        Promise promise = servicePromise.get("test", 1_500L);
        promise
                .append("1", (_, _) -> System.out.println(1))
                .append("2", (_, _) -> System.out.println(2))
                .then("3", (_, _) -> {
                    throw new RuntimeException("Test");
                })
                .run()
                .await(1000);
        System.out.println(promise.getLogString());
    }

    @Test
    void testOneTaskExecutionTime() {
        AtomicInteger x = new AtomicInteger(0);
        Promise promise = servicePromise.get("testOneTaskExecutionTime", 1_500L);
        promise
                .append("1", (_, _) -> {

                })
                .onComplete((_, _) -> x.incrementAndGet())
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
                .onComplete((_, _) -> x.incrementAndGet())
                .run()
                .await(1000);
        Assertions.assertEquals(1, x.get());
    }

    @Test
    void testExternalWait() {
        Promise promise = servicePromise.get("Async", 6_000L);
        PromiseTask promiseTask = new PromiseTask("test", promise, PromiseTaskExecuteType.EXTERNAL_WAIT);
        promise.append(promiseTask);
        promise.run().await(1000);

        Assertions.assertEquals(1, promise.getTrace().size());
        Assertions.assertEquals(0, promise.getExceptionTrace().size());
        Assertions.assertTrue(promise.isRun());

        promiseTask.externalComplete();
        Assertions.assertFalse(promise.isRun());
        Assertions.assertEquals(2, promise.getTrace().size());

        System.out.println(promise.getLogString());
    }

    @Test
    void testAsyncNoWait() {

        // Наболюдал картину "ERROR" залогировалось в консоле
        // Но проверка Assertions.assertEquals(1, c.get()); не прошла
        // c.get() = 0 без малейшего понятия как такое могло получится
        // добавил final c + c.incrementAndGet() уже равно не 1 а 2
        // Буду наблюдать дальше

        Promise promise = servicePromise.get("AsyncNoWait", 6_000L);
        PromiseTask promiseTask = new PromiseTask("test", promise, PromiseTaskExecuteType.ASYNC_NO_WAIT_IO, (_, _) -> {
            throw new RuntimeException("ERROR");
        });
        promise.append(promiseTask);
        promise.run().await(1000);
        System.out.println(promise.getLogString());

        Assertions.assertFalse(promise.isRun());
    }

    @Test
    void testExpiration() {
        Promise promise = servicePromise.get("Expiration", 1_000L);
        AtomicInteger counter = new AtomicInteger(0);
        promise
                .append("longTimeout", (_, _)
                        -> Util.sleepMs(2000))
                .onError((_, _) -> counter.incrementAndGet())
                .run().await(2010);

        System.out.println(promise.getLogString());

        Assertions.assertFalse(promise.isRun());
        Assertions.assertTrue(promise.isException());
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals("TimeOut cause: ServicePromise.onPromiseTaskExpired", promise.getExceptionTrace().getFirst().getIndex());

    }

    @SuppressWarnings("unused")
    void promiseTaskWithPool() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", HttpResource.class, (_, _, _) -> {
                    //HttpResponseEnvelope execute = httpClientResource.execute(new Http2ClientImpl());

                })
                .appendWithResource("jdbc", JdbcResource.class, (_, _, jdbcResource)
                        -> System.out.println(jdbcResource))
                .run()
                .await(2000);
        System.out.println(promise.getLogString());
    }

    @Test
    void appendBeforeRun() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise.append("test", (_, promise1)
                -> promise1.append("hey", (_, _) -> {
        }));
        promise.run().await(1000);
        System.out.println(promise.getLogString());
        Assertions.assertTrue(promise.isException());
    }

    @Test
    void waitBeforeExternalTask() {
        Promise promise = servicePromise.get("testPromise", 1_000L);
        promise
                .append("st", (_, promise1) -> {
                    PromiseTask asyncPromiseTask = new PromiseTask(
                            "async",
                            promise1,
                            PromiseTaskExecuteType.EXTERNAL_WAIT
                    );
                    List<PromiseTask> add = new ArrayList<>();
                    add.add(asyncPromiseTask);
                    add.add(new PromiseTask(PromiseTaskExecuteType.WAIT.getNameCamel(), promise1, PromiseTaskExecuteType.WAIT));
                    promise1.addToHead(add);
                })
                .run()
                .await(2000);

        System.out.println(promise.getLogString());
        //Мы по timeout должны упасть
        Assertions.assertTrue(promise.isException());
        // Мы дали 1 секунду время жизни, EXTERNAL_WAIT не финишировал -> сработал timeout
        Assertions.assertFalse(promise.isRun());

    }

    @Test
    void testGoTo() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise
                .then("1task", (_, promise1) -> promise1.goTo("task3"))
                .then("task2", (_, _) -> xx.incrementAndGet())
                .then("task3", (_, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(1, xx.get());
    }

    @Test
    void testGoTo2() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise
                .then("1task", (_, promise1) -> promise1.goTo("task5"))
                .then("task2", (_, _) -> xx.incrementAndGet())
                .then("task3", (_, _) -> xx.incrementAndGet())
                .then("task4", (_, _) -> xx.incrementAndGet())
                .then("task5", (_, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(1, xx.get());
    }

    @Test
    void testGoTo3() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise.then("1task", (_, promise1) -> {
            // Сначала добежим до task3 и выполним
            // потом пробежим до task5 и выполним
            promise1.goTo("task3");
            promise1.goTo("task5");
                })
                .then("task2", (_, _) -> xx.incrementAndGet())
                .then("task3", (_, _) -> xx.incrementAndGet())
                .then("task4", (_, _) -> xx.incrementAndGet())
                .then("task5", (_, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(2, xx.get());
    }

    @Test
    void testGoToError() {
        AtomicInteger xx = new AtomicInteger(0);
        Promise promise = servicePromise.get("goTo", 6_000L);
        promise.then("1task", (_, promise1) -> {
            // Сначала добежим до task3 и выполним
            // потом пробежим до task5 и выполним
            promise1.goTo("task3");
            promise1.goTo("task6");
                })
                .then("task2", (_, _) -> xx.incrementAndGet())
                .then("task3", (_, _) -> xx.incrementAndGet())
                .then("task4", (_, _) -> xx.incrementAndGet())
                .then("task5", (_, _) -> xx.incrementAndGet());
        promise.run().await(1000);
        System.out.println(promise.getLogString());
        Assertions.assertEquals(1, xx.get());
        Assertions.assertTrue(promise.isException());
    }

}