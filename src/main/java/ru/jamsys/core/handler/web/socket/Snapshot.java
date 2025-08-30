package ru.jamsys.core.handler.web.socket;

import lombok.Getter;
import ru.jamsys.core.handler.web.socket.snapshot.Operation;
import ru.jamsys.core.handler.web.socket.snapshot.OperationClient;
import ru.jamsys.core.handler.web.socket.snapshot.ServerCommit;
import ru.jamsys.core.handler.web.socket.snapshot.SnapshotObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class Snapshot {

    private final Map<String, SnapshotObject> objects = new ConcurrentHashMap<>();
    private final AtomicInteger serial = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    public Operation accept(OperationClient operationClient, String idUser) {
        Operation resultOperation = new Operation(operationClient);
        switch (resultOperation.getOperationClient().getOperationType()) {
            case CREATE -> {
                AtomicBoolean result = new AtomicBoolean(false);
                SnapshotObject snapshotObject = objects.computeIfAbsent(operationClient.getUuidObject(), uuid -> {
                    result.set(true);
                    return new SnapshotObject(uuid);
                });
                if (result.get()) {
                    resultOperation.setServerCommit(new ServerCommit(
                            true,
                            this.serial.getAndIncrement(),
                            idUser,
                            null,
                            operationClient.getUuidObject()
                    ));
                    snapshotObject.accept(resultOperation);
                    operations.add(resultOperation);
                } else {
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "duplicate " + operationClient.getUuidObject(),
                            null
                    ));
                }
            }
            case UPDATE, DELETE -> {
                SnapshotObject snapshotObject = objects.get(operationClient.getUuidObject());
                if (snapshotObject == null) {
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "not found " + operationClient.getUuidObject(),
                            null
                    ));
                } else {
                    if (snapshotObject.getToken().compareAndSet(
                            operationClient.getTokenForUpdate(),
                            java.util.UUID.randomUUID().toString())
                    ) {
                        resultOperation.setServerCommit(new ServerCommit(
                                true,
                                this.serial.getAndIncrement(),
                                idUser,
                                null,
                                snapshotObject.getToken().get()
                        ));
                        snapshotObject.accept(resultOperation);
                        operations.add(resultOperation);
                    } else {
                        resultOperation.setServerCommit(new ServerCommit(
                                false,
                                -1,
                                idUser,
                                "invalid token " + operationClient.getTokenForUpdate(),
                                null
                        ));
                    }
                }
            }

        }
        return resultOperation;
    }

}
