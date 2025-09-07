package ru.jamsys.core.handler.web.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.handler.web.socket.operation.Operation;
import ru.jamsys.core.handler.web.socket.operation.OperationClient;
import ru.jamsys.core.handler.web.socket.operation.OperationType;

import java.util.HashMap;
import java.util.Map;

class OperationRepositoryTest {

    @Test
    public void x() {
        OperationRepository operationRepository = new OperationRepository();
        Operation op1 = operationRepository.accept(getUserOperation(OperationType.PUT, "1", "1", new HashMapBuilder<String, Object>().append("x", "y1")), "1");

        Assertions.assertTrue(op1.getServerCommit().isCommit());
        Assertions.assertEquals(1, op1.getServerCommit().getId());

        Operation op2 = operationRepository.accept(getUserOperation(OperationType.PUT, "1", "1", null), "1");
        Assertions.assertFalse(op2.getServerCommit().isCommit());
        Assertions.assertNotNull(op2.getServerCommit().getReplaceOperationObject());
        Assertions.assertEquals(-1, op2.getServerCommit().getId());

        Operation op3 = operationRepository.accept(getUserOperation(OperationType.PUT, "1", op1.getServerCommit().getNewTokenForUpdate(), new HashMapBuilder<String, Object>().append("x", "y2")), "1");
        Assertions.assertTrue(op3.getServerCommit().isCommit());
        Assertions.assertEquals(2, op3.getServerCommit().getId());

        Operation op4 = operationRepository.accept(getUserOperation(OperationType.REMOVE, "1", "1", null), "1");
        Assertions.assertFalse(op4.getServerCommit().isCommit());
        Assertions.assertEquals(-1, op4.getServerCommit().getId());

        Operation op5 = operationRepository.accept(getUserOperation(OperationType.REMOVE, "1", op3.getServerCommit().getNewTokenForUpdate(), new HashMap<>()), "1");
        Assertions.assertTrue(op5.getServerCommit().isCommit());
        Assertions.assertEquals(3, op5.getServerCommit().getId());

        Operation op6 = operationRepository.accept(getUserOperation(OperationType.PUT, "2", "2", null), "1");
        Assertions.assertTrue(op6.getServerCommit().isCommit());
        Assertions.assertEquals(4, op6.getServerCommit().getId());

        UtilLog.printInfo(operationRepository);
    }

    @Test
    public void serial() {
        OperationRepository operationRepository = new OperationRepository();

        operationRepository.accept(getUserOperation(OperationType.PUT, "1", "1", new HashMapBuilder<String, Object>().append("x", "y1")), "1");
        Operation op2 = operationRepository.accept(getUserOperation(OperationType.PUT, "2", "2", new HashMapBuilder<String, Object>().append("x", "y1")), "1");
        operationRepository.accept(getUserOperation(OperationType.PUT, "3", "3", new HashMapBuilder<String, Object>().append("x", "y1")), "1");

        operationRepository.accept(getUserOperation(OperationType.REMOVE, "2", op2.getServerCommit().getNewTokenForUpdate(), null), "1");

        Assertions.assertEquals(2, operationRepository.getActiveObjectsKeySerial().size());
        Assertions.assertEquals("1", operationRepository.getActiveObjectsKeySerial().getFirst());
        Assertions.assertEquals("3", operationRepository.getActiveObjectsKeySerial().getLast());
    }

    @Test
    public void delete() {
        OperationRepository operationRepository = new OperationRepository();
        Operation op1 = operationRepository.accept(getUserOperation(OperationType.PUT, "1", "1", new HashMapBuilder<String, Object>().append("x", "y1")), "1");
        Assertions.assertTrue(op1.getServerCommit().isCommit());
        Assertions.assertEquals(1, op1.getServerCommit().getId());

        Operation op5 = operationRepository.accept(getUserOperation(OperationType.REMOVE, "1", op1.getServerCommit().getNewTokenForUpdate(), new HashMap<>()), "1");
        Assertions.assertTrue(op5.getServerCommit().isCommit());

        UtilLog.printInfo(operationRepository);
    }

    private OperationClient getUserOperation(
            OperationType operationType,
            String uuidOperationObject,
            String token,
            Map<String, Object> data
    ) {
        return new OperationClient(
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                operationType,
                token,
                uuidOperationObject,
                data
        );
    }

    @Test
    public void convert() throws JsonProcessingException {
        String json = """
                {
                  "uuid": "12345",
                  "timestampAdd": 1693838321,
                  "operationType": "PUT",
                  "tokenForUpdate": "abc123",
                  "uuidOperationObject": "obj-789",
                  "data": {
                    "field1": "value1",
                    "field2": 42
                  }
                }
                """;
        OperationClient operationClient = OperationClient.fromJson(json);
        Assertions.assertEquals("12345", operationClient.getUuid());
    }

    @Test
    public void convert2() throws JsonProcessingException {
        String json = """
                {
                    "xx": "yy",
                    "message": {
                      "uuid": "12345",
                      "timestampAdd": 1693838321,
                      "operationType": "PUT",
                      "tokenForUpdate": "abc123",
                      "uuidOperationObject": "obj-789",
                      "data": {
                        "field1": "value1",
                        "field2": 42
                      }
                    }
                }
                """;
        OperationClient operationClient = OperationClient.fromJson(json, "/message");
        Assertions.assertEquals("12345", operationClient.getUuid());
    }

    @Test
    public void convert3() throws Throwable {
        String json = """
                {
                    "xx": "yy",
                    "message": {
                      "uuid": "12345",
                      "timestampAdd": 1693838321,
                      "operationType": "PUT",
                      "tokenForUpdate": "abc123",
                      "uuidOperationObject": "obj-789",
                      "data": {
                        "field1": "value1",
                        "field2": 42
                      }
                    }
                }
                """;
        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(json);
        OperationClient operationClient = OperationClient.fromMap((Map<String, Object>) mapOrThrow.get("message"));
        Assertions.assertEquals("12345", operationClient.getUuid());
    }

}