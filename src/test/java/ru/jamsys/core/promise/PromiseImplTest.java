package ru.jamsys.core.promise;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.promise.api.YandexSpeechPromise;
import ru.jamsys.core.util.Util;

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

    @Test
    void test1() {
        PromiseImpl wf = new PromiseImpl("test");
        wf
                .append("test", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H1");
                    ArrayList<PromiseTask> objects = new ArrayList<>();
                    objects.add(new PromiseTask("test2", wf, PromiseTaskType.JOIN, (AtomicBoolean _) -> System.out.println(Thread.currentThread().getName() + " EXTRA")));
                    return objects;
                })
                .append("test", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H2");

                })
                .then("test", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    Util.sleepMs(1000);
                    System.out.println(Thread.currentThread().getName() + " H3");
                    return null;
                })
                .then("test", PromiseTaskType.JOIN, (AtomicBoolean _) -> System.out.println(Thread.currentThread().getName() + " FINISH"))
                .run()
                .await(4000);
    }

    @Test
    void test2() {
        PromiseImpl wf = new PromiseImpl("test");
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            wf.then("test", PromiseTaskType.JOIN, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run();
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test3() {
        PromiseImpl wf = new PromiseImpl("test");
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 1000; i++) {
            final int x = i;
            wf.then("test", PromiseTaskType.JOIN, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run();
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test4() {
        PromiseImpl wf = new PromiseImpl("test");
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            wf.then("test", PromiseTaskType.IO, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        wf.run();
        Util.sleepMs(1000);
        Assertions.assertEquals(dequeRes.toString(), deque.toString());
    }

    @Test
    void test5() {
        PromiseImpl wf = new PromiseImpl("test");
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<Integer> dequeRes = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < 1000; i++) {
            final int x = i;
            wf.then("test", PromiseTaskType.IO, (AtomicBoolean _) -> deque.add(x));
            dequeRes.add(i);
        }
        //System.out.println("start size: " + wf.getListPendingTasks().size());
        wf.run();
        wf.await(5000);
        //System.out.println("time: " + (System.currentTimeMillis() - start));
        //System.out.println("fin size: " + wf.getListPendingTasks().size());
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
        Promise wf = new PromiseImpl("test");
        AtomicInteger retry = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);
        AtomicInteger complete = new AtomicInteger(0);
        wf
                .append("test", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    retry.incrementAndGet();
                    throw new RuntimeException("Hello world");
                })
                .getLastAppendedTask().setRetryCount(1, 1000).getPromise()
                .onError((Throwable _) -> error.incrementAndGet())
                .onComplete(complete::incrementAndGet)
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

        Promise wf = new PromiseImpl("test");
        wf.setTimeOut(1_500);
        wf
                .append("1", PromiseTaskType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("2", PromiseTaskType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .then("3", PromiseTaskType.JOIN, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError((Throwable _) -> error.incrementAndGet())
                .onComplete(complete::incrementAndGet)
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

        Promise wf = new PromiseImpl("test");
        wf.setTimeOut(1_500);
        wf
                .append("1", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .append("2", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1500);
                })
                .then("3", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    exec.incrementAndGet();
                    Util.sleepMs(1000);
                })
                .onError((Throwable _) -> error.incrementAndGet())
                .onComplete(complete::incrementAndGet)
                .run()
                .await(2000);

        Assertions.assertEquals(1, error.get());
        Assertions.assertEquals(0, complete.get());
        Assertions.assertEquals(2, exec.get());
    }

    @Test
    void toLog() {
        Promise wf = new PromiseImpl("test");
        wf.setTimeOut(1_500);
        wf
                .append("1", PromiseTaskType.IO, (AtomicBoolean _) -> System.out.println(1))
                .append("2", PromiseTaskType.IO, (AtomicBoolean _) -> System.out.println(2))
                .then("3", PromiseTaskType.IO, (AtomicBoolean _) -> {
                    throw new RuntimeException("Test");
                })
                .run()
                .await(1000);
        System.out.println(wf.getLog());
    }

    @Test
    void testAsync() {
        Promise wf = new PromiseImpl("test");
        wf.setIndex("Async");
        PromiseTask promiseTask = new PromiseTask("test", wf, PromiseTaskType.ASYNC);
        wf.append(promiseTask);
        wf.run().await(1000);

        Assertions.assertEquals(1, wf.getTrace().size());
        Assertions.assertEquals(0, wf.getExceptionTrace().size());
        Assertions.assertFalse(wf.isCompleted());

        promiseTask.complete();
        Assertions.assertTrue(wf.isCompleted());
        Assertions.assertEquals(2, wf.getTrace().size());

        System.out.println(wf.getLog());

    }

    @SuppressWarnings("unused")
    void promiseYandexSpeechKit() {
        Promise wf = new PromiseImpl("test");
        wf.api("sound", new YandexSpeechPromise().setup((YandexSpeechPromise yandexSpeechPromise) -> {
            yandexSpeechPromise.setText("Привет страна");
            yandexSpeechPromise.setFilePath("target/result2.wav");
        })).run().await(10000);
        System.out.println(wf.getLog());
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

}