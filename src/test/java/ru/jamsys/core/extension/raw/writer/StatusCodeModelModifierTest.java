package ru.jamsys.core.extension.raw.writer;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilLog;

class StatusCodeModelModifierTest {

    @Getter
    public enum TestStatusCodeModelModel implements StatusCodeModel {
        LOGGER(0, "Read Logger"),
        STATISTIC(1, "Read Statistic"),
        STATISTIC2(15, "Read Statistic"),
        ;

        private final int byteIndex;
        private final String description;

        // Конструктор enum (приватный)
        TestStatusCodeModelModel(int byteIndex, String description) {
            this.byteIndex = byteIndex;
            this.description = description;
        }

    }

    @Test
    void test() {
        StatusCode<TestStatusCodeModelModel> statusCode = new StatusCode<>((short) 0, TestStatusCodeModelModel.class);
        UtilLog.printInfo(StatusCodeModelModifierTest.class, statusCode);

        Assertions.assertFalse(statusCode.getStatus(TestStatusCodeModelModel.STATISTIC));
        Assertions.assertFalse(statusCode.getStatus(TestStatusCodeModelModel.STATISTIC));
        Assertions.assertEquals(0, statusCode.getStatusCode());

        statusCode.set(TestStatusCodeModelModel.LOGGER, true);

        Assertions.assertTrue(statusCode.getStatus(TestStatusCodeModelModel.LOGGER));
        Assertions.assertFalse(statusCode.getStatus(TestStatusCodeModelModel.STATISTIC));
        Assertions.assertEquals(1, statusCode.getStatusCode());

        UtilLog.printInfo(StatusCodeModelModifierTest.class, statusCode);

        statusCode.set(TestStatusCodeModelModel.STATISTIC2, true);
        UtilLog.printInfo(StatusCodeModelModifierTest.class, statusCode);
        Assertions.assertEquals(-32767, statusCode.getStatusCode());

    }
}