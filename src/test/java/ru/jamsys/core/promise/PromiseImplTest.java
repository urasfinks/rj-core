package ru.jamsys.core.promise;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

class PromiseImplTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.run(args);
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test1() {
        Promise promise = servicePromise.get("test", 6_000L); //new PromiseImpl("test", 6_000L);
        promise
                .append("test", (_, promise1) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H1");
                    ArrayList<PromiseTask> objects = new ArrayList<>();
                    objects.add(new PromiseTask("test2", promise, PromiseTaskExecuteType.JOIN, (_, _) -> System.out.println(Thread.currentThread().getName() + " EXTRA")));
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
                .join("test", (_, _) -> System.out.println(Thread.currentThread().getName() + " FINISH"))
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
            promise.join("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run();
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3() {
        Promise promise = servicePromise.get("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();

        for (int i = 0; i < 1000; i++) {
            final int x = i;
            promise.join("test", (_, _) -> deque.add(x));
            dequeRes.add(i);
        }
        promise.run().await(1100);
        System.out.println("test3 isTerminated: " + promise.isTerminated());
        System.out.println(promise.getLog());
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
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
        promise.run();
        Util.sleepMs(1000);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test5() {
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
                .getLastAppendedTask().setRetryCount(1, 1000).getPromise()
                .onError((_, _) -> error.incrementAndGet())
                .onComplete((_, _) -> complete.incrementAndGet())
                .run()
                .await(3000);
        System.out.println(promise.getLog());
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
                .join("1", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .join("2", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .join("3", (_, _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError((_, _) -> error.incrementAndGet())
                .onComplete((_, _) -> complete.incrementAndGet())
                .run()
                .await(2000);
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
        System.out.println(promise.getLog());
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
        System.out.println(promise.getLog());
    }

    @Test
    void testExternalWait() {
        Promise promise = servicePromise.get("Async", 6_000L);
        PromiseTask promiseTask = new PromiseTask("test", promise, PromiseTaskExecuteType.EXTERNAL_WAIT);
        promise.append(promiseTask);
        promise.run().await(1000);

        Assertions.assertEquals(1, promise.getTrace().size());
        Assertions.assertEquals(0, promise.getExceptionTrace().size());
        Assertions.assertFalse(promise.isTerminated());

        promiseTask.externalComplete();
        Assertions.assertTrue(promise.isTerminated());
        Assertions.assertEquals(2, promise.getTrace().size());

        System.out.println(promise.getLog());
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
        System.out.println(promise.getLog());

        Assertions.assertTrue(promise.isTerminated());
    }

    @Test
    void testExpiration() {
        Promise promise = servicePromise.get("Expiration", 1_000L);
        AtomicInteger counter = new AtomicInteger(0);
        promise
                .append("longTimeout", (_, _)
                        -> Util.sleepMs(2000)).onError((_, _) -> counter.incrementAndGet())
                .run().await(2010);

        System.out.println(promise.getLog());

        Assertions.assertTrue(promise.isTerminated());
        Assertions.assertTrue(promise.isException());
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals("TimeOut cause: PromiseComponent.onPromiseTaskExpired", promise.getExceptionTrace().getFirst().getIndex());

    }

    @SuppressWarnings("unused")
    void promiseTaskWithPool() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", HttpResource.class, (_, _, _) -> {
                    //HttpResponseEnvelope execute = httpClientResource.execute(new Http2ClientImpl());
                    System.out.println("!!!");
                })
                .appendWithResource("jdbc", JdbcResource.class, (_, _, jdbcResource)
                        -> System.out.println(jdbcResource))
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }

    @Test
    void appendBeforeRun() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise.append("test", (_, promise1)
                -> promise1.append("hey", (_, _) -> {
        }));
        promise.run().await(1000);
        System.out.println(promise.getLog());
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
                    add.add(new PromiseTask(PromiseTaskExecuteType.WAIT.getName(), promise1, PromiseTaskExecuteType.WAIT));
                    promise1.addToHead(add);
                })
                .run()
                .await(2000);

        System.out.println(promise.getLog());
        //Мы по timeout должны упасть
        Assertions.assertTrue(promise.isException());
        // Мы дали 1 секунду время жизни, EXTERNAL_WAIT не финишировал -> сработал timeout
        Assertions.assertTrue(promise.isTerminated());

    }

}