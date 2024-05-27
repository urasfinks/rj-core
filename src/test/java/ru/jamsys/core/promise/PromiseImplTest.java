package ru.jamsys.core.promise;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.resource.extension.YandexSpeechPromise;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class PromiseImplTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test1() {
        PromiseImpl wf = new PromiseImpl("test", 6_000L);
        wf
                .append("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H1");
                    ArrayList<PromiseTask> objects = new ArrayList<>();
                    objects.add(new PromiseTask("test2", wf, PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> System.out.println(Thread.currentThread().getName() + " EXTRA")));
                    return objects;
                })
                .append("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H2");

                })
                .then("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H3");
                    return null;
                })
                .then("test", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> System.out.println(Thread.currentThread().getName() + " FINISH"))
                .run()
                .await(4000);
    }

    @Test
    void test2() {
        PromiseImpl wf = new PromiseImpl("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            wf.then("test", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run();
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3() {
        PromiseImpl wf = new PromiseImpl("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();

        for (int i = 0; i < 1000; i++) {
            final int x = i;
            wf.then("test", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run().await(1100);
        System.out.println("test3 isTerminated: " + wf.isTerminated() + "; any: " + wf.getAny());
        System.out.println(wf.getLog());
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test4() {
        PromiseImpl wf = new PromiseImpl("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            wf.then("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run();
        Util.sleepMs(1000);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test5() {
        PromiseImpl wf = new PromiseImpl("test", 6_000L);
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 1000; i++) {
            final int x = i;
            wf.then("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        //System.out.println("start size: " + wf.getListPendingTasks().size());
        wf.run();
        wf.await(5000);
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
        Promise wf = new PromiseImpl("test", 6_000L);
        AtomicInteger retry = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        wf
                .append("test", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    retry.incrementAndGet();
                    throw new RuntimeException("Hello world");
                })
                .getLastAppendedTask().setRetryCount(1, 1000).getPromise()
                .onError(PromiseTaskExecuteType.JOIN, _ -> error.incrementAndGet())
                .onComplete(PromiseTaskExecuteType.JOIN, _ -> complete.incrementAndGet())
                .run()
                .await(3000);
        System.out.println(wf.getLog());
        Assertions.assertEquals(2, retry.get());
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());

    }

    @Test
    void testTimeOut() {
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        AtomicInteger exec = new AtomicInteger(0);

        Promise wf = new PromiseImpl("test", 1_500L);
        wf
                .append("1", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("2", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("3", PromiseTaskExecuteType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError(PromiseTaskExecuteType.JOIN, _ -> error.incrementAndGet())
                .onComplete(PromiseTaskExecuteType.JOIN, _ -> complete.incrementAndGet())
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

        Promise wf = new PromiseImpl("test", 1_500L);
        wf
                .append("1", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .append("2", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1500);
                })
                .then("3", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError(PromiseTaskExecuteType.JOIN, _ -> error.incrementAndGet())
                .onComplete(PromiseTaskExecuteType.JOIN, _ -> complete.incrementAndGet())
                .run()
                .await(2000);
        System.out.println(wf.getLog());
        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
        Assertions.assertEquals(2, exec.get());
    }

    @Test
    void toLog() {
        Promise wf = new PromiseImpl("test", 1_500L);
        wf
                .append("1", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> System.out.println(1))
                .append("2", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> System.out.println(2))
                .then("3", PromiseTaskExecuteType.IO, (AtomicBoolean _) -> {
                    throw new RuntimeException("Test");
                })
                .run()
                .await(1000);
        System.out.println(wf.getLog());
    }

    @Test
    void testExternalWait() {
        Promise wf = new PromiseImpl("Async", 6_000L);
        PromiseTask promiseTask = new PromiseTask("test", wf, PromiseTaskExecuteType.EXTERNAL_WAIT);
        wf.append(promiseTask);
        wf.run().await(1000);

        Assertions.assertEquals(1, wf.getTrace().size());
        Assertions.assertEquals(0, wf.getExceptionTrace().size());
        Assertions.assertFalse(wf.isTerminated());

        promiseTask.externalComplete();
        Assertions.assertTrue(wf.isTerminated());
        Assertions.assertEquals(2, wf.getTrace().size());

        System.out.println(wf.getLog());
    }

    @Test
    void testAsyncNoWait() {

        // Наболюдал картину "ERROR" залогировалось в консоле
        // Но проверка Assertions.assertEquals(1, c.get()); не прошла
        // c.get() = 0 без малейшего понятия как такое могло получится
        // добавил final c + c.incrementAndGet() уже равно не 1 а 2
        // Буду наблюдать дальше

        Promise wf = new PromiseImpl("AsyncNoWait", 6_000L);
        final AtomicInteger c = new AtomicInteger(0);
        PromiseTask promiseTask = new PromiseTask("test", wf, PromiseTaskExecuteType.ASYNC_NO_WAIT_IO, (AtomicBoolean _) -> {
            c.incrementAndGet();
            throw new RuntimeException("ERROR");
        });
        wf.append(promiseTask);
        wf.run().await(1000);
        System.out.println(wf.getLog());

        Assertions.assertEquals(2, c.incrementAndGet());
        Assertions.assertEquals(1, wf.getTrace().size());
        Assertions.assertTrue(wf.isTerminated());
        Assertions.assertEquals(1, wf.getTrace().size());
    }

    @Test
    void testExpiration() {
        Promise wf = new PromiseImpl("Expiration", 1_000L);
        AtomicInteger counter = new AtomicInteger(0);
        wf
                .append("longTimeout", PromiseTaskExecuteType.IO, _ -> {
                    Util.sleepMs(2000);
                }).onError(PromiseTaskExecuteType.JOIN, _ -> counter.incrementAndGet())
                .run().await(2000);

        System.out.println(wf.getLog());

        Assertions.assertTrue(wf.isTerminated());
        Assertions.assertTrue(wf.isException());
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals("TimeOut cause: PromiseTaskTime::onPromiseTaskExpired", wf.getExceptionTrace().getFirst().getIndex());

    }

    @SuppressWarnings("unused")
    void promiseYandexSpeechKit() {
        Promise wf = new PromiseImpl("test", 6_000L);
        wf.extension("sound", new YandexSpeechPromise().setup((YandexSpeechPromise yandexSpeechPromise) -> {
            yandexSpeechPromise.setText("Привет страна");
            yandexSpeechPromise.setFilePath("target/result2.wav");
        })).run().await(10000);
        System.out.println(wf.getLog());
    }

    @SuppressWarnings("unused")
    void promiseTaskWithPool() {
        Promise promise = new PromiseImpl("testPromise", 6_000L);
        promise
                .appendWithResource("http", HttpResource.class, (_, _) -> {
                    //HttpResponseEnvelope execute = httpClientResource.execute(new Http2ClientImpl());
                    System.out.println("!!!");
                })
                .appendWithResource("jdbc", JdbcResource.class, (_, jdbcResource) -> {
                    System.out.println(jdbcResource);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }

}