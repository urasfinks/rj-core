package ru.jamsys.core.extension.raw.writer;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilLog;

class SubscriberStatusReadModelModifierTest {

    @Getter
    public enum TestSubscriberStatusReadModelModel implements SubscriberStatusReadModel {
        LOGGER(0, "Read Logger"),
        STATISTIC(1, "Read Statistic"),
        STATISTIC2(15, "Read Statistic"),
        ;

        private final int byteIndex;
        private final String description;

        // Конструктор enum (приватный)
        TestSubscriberStatusReadModelModel(int byteIndex, String description) {
            this.byteIndex = byteIndex;
            this.description = description;
        }

    }

    @Test
    void test() {
        SubscriberStatusRead<TestSubscriberStatusReadModelModel> subscriberStatusRead = new SubscriberStatusRead<>((short) 0, TestSubscriberStatusReadModelModel.class);
        UtilLog.printInfo(subscriberStatusRead);

        Assertions.assertFalse(subscriberStatusRead.getStatus(TestSubscriberStatusReadModelModel.STATISTIC));
        Assertions.assertFalse(subscriberStatusRead.getStatus(TestSubscriberStatusReadModelModel.STATISTIC));
        Assertions.assertEquals(0, subscriberStatusRead.getSubscriberStatusRead());

        subscriberStatusRead.set(TestSubscriberStatusReadModelModel.LOGGER, true);

        Assertions.assertTrue(subscriberStatusRead.getStatus(TestSubscriberStatusReadModelModel.LOGGER));
        Assertions.assertFalse(subscriberStatusRead.getStatus(TestSubscriberStatusReadModelModel.STATISTIC));
        Assertions.assertEquals(1, subscriberStatusRead.getSubscriberStatusRead());

        UtilLog.printInfo(subscriberStatusRead);

        subscriberStatusRead.set(TestSubscriberStatusReadModelModel.STATISTIC2, true);
        UtilLog.printInfo(subscriberStatusRead);
        Assertions.assertEquals(-32767, subscriberStatusRead.getSubscriberStatusRead());

    }
}