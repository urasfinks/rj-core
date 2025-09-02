package ru.jamsys.core.handler.web.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.handler.web.socket.operation.Operation;
import ru.jamsys.core.handler.web.socket.operation.OperationClient;
import ru.jamsys.core.handler.web.socket.operation.OperationType;

import java.util.HashMap;
import java.util.Map;

class SnapshotOperationObjectTest {

    @Test
    public void x() {
        SnapshotOperationObject snapshotOperationObject = new SnapshotOperationObject();
        Operation op1 = snapshotOperationObject.accept(getUserOperation(OperationType.CREATE, "1", null, new HashMapBuilder<String, Object>().append("x", "y1")), "1");
        Assertions.assertTrue(op1.getServerCommit().isCommit());
        Assertions.assertEquals(1, op1.getServerCommit().getId());

        Operation op2 = snapshotOperationObject.accept(getUserOperation(OperationType.CREATE, "1", null, null), "1");
        Assertions.assertFalse(op2.getServerCommit().isCommit());
        Assertions.assertEquals(-1, op2.getServerCommit().getId());

        Operation op3 = snapshotOperationObject.accept(getUserOperation(OperationType.UPDATE, "1", "1", new HashMapBuilder<String, Object>().append("x", "y2")), "1");
        Assertions.assertTrue(op3.getServerCommit().isCommit());
        Assertions.assertEquals(2, op3.getServerCommit().getId());

        Operation op4 = snapshotOperationObject.accept(getUserOperation(OperationType.DELETE, "1", "1", null), "1");
        Assertions.assertFalse(op4.getServerCommit().isCommit());
        Assertions.assertEquals(-1, op4.getServerCommit().getId());
        Assertions.assertEquals("invalid token", op4.getServerCommit().getCause());

        Operation op5 = snapshotOperationObject.accept(getUserOperation(OperationType.DELETE, "1", op3.getServerCommit().getNewTokenForUpdate(), new HashMap<>()), "1");
        Assertions.assertTrue(op5.getServerCommit().isCommit());
        Assertions.assertEquals(3, op5.getServerCommit().getId());

        Operation op6 = snapshotOperationObject.accept(getUserOperation(OperationType.UPDATE, "2", "2", null), "1");
        Assertions.assertFalse(op6.getServerCommit().isCommit());
        Assertions.assertEquals("not found 2", op6.getServerCommit().getCause());


        UtilLog.printInfo(snapshotOperationObject);
    }

    @Test
    public void delete() {
        SnapshotOperationObject snapshotOperationObject = new SnapshotOperationObject();
        Operation op1 = snapshotOperationObject.accept(getUserOperation(OperationType.CREATE, "1", null, new HashMapBuilder<String, Object>().append("x", "y1")), "1");
        Assertions.assertTrue(op1.getServerCommit().isCommit());
        Assertions.assertEquals(1, op1.getServerCommit().getId());

        Operation op5 = snapshotOperationObject.accept(getUserOperation(OperationType.DELETE, "1", "1", new HashMap<>()), "1");
        Assertions.assertTrue(op5.getServerCommit().isCommit());

        UtilLog.printInfo(snapshotOperationObject);
    }

    private OperationClient getUserOperation(
            OperationType operationType,
            String uuidObject,
            String token,
            Map<String, Object> data
    ) {
        return new OperationClient(
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                operationType,
                token,
                uuidObject,
                data
        );
    }

}