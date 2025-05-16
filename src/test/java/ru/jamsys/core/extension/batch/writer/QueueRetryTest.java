package ru.jamsys.core.extension.batch.writer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;

import java.util.concurrent.atomic.AtomicBoolean;

class QueueRetryTest {

    AtomicBoolean threadRun = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056

        QueueRetry test = new QueueRetry("test");

        test.add(0, null, "Hello");
        Assertions.assertEquals(1, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());

        DataPayload dataPayload = test.pollLast(1_000, curTimeMs);
        Assertions.assertTrue(test.queueIsEmpty());
        Assertions.assertEquals(0, test.size());
        Assertions.assertFalse(test.getExpirationListConfiguration().get().isEmpty());

        test.getExpirationListConfiguration().get().helper(threadRun, curTimeMs + 2000);

        // Должно произойти протухание и обратно в queue вставиться
        Assertions.assertEquals(1, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());

        DataPayload dataPayload2 = test.pollLast(1_500, curTimeMs);
        // Это 2 одинаковых объекта
        Assertions.assertEquals(dataPayload, dataPayload2);
        Assertions.assertTrue(test.queueIsEmpty());
        Assertions.assertEquals(0, test.size());
        Assertions.assertFalse(test.getExpirationListConfiguration().get().isEmpty());
        Assertions.assertFalse(test.isEmpty());

        // Теперь делаю человеческое удаление
        test.commit(dataPayload2);
        test.getExpirationListConfiguration().get().helper(threadRun, curTimeMs + 2000);

        // Теперь ничего не должно нигде остаться
        Assertions.assertTrue(test.queueIsEmpty());
        Assertions.assertEquals(0, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());
        Assertions.assertTrue(test.isEmpty());

    }

}