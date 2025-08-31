package ru.jamsys.core.handler.web.socket;

import lombok.Getter;
import ru.jamsys.core.handler.web.socket.snapshot.Operation;
import ru.jamsys.core.handler.web.socket.snapshot.OperationClient;
import ru.jamsys.core.handler.web.socket.snapshot.ServerCommit;
import ru.jamsys.core.handler.web.socket.snapshot.SnapshotObject;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public class Snapshot {

    private final Map<String, SnapshotObject> objects = new ConcurrentHashMap<>();
    private final AtomicInteger serial = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    public Operation accept(OperationClient operationClient, String idUser) {
        Operation resultOperation = new Operation(operationClient);

        switch (resultOperation.getOperationClient().getOperationType()) {
            case CREATE -> {
                AtomicBoolean created = new AtomicBoolean(false);
                SnapshotObject snapshotObject = objects.computeIfAbsent(
                        operationClient.getUuidObject(),
                        uuid -> {
                            created.set(true);
                            return new SnapshotObject(uuid);
                        }
                );

                if (created.get()) {
                    resultOperation.setServerCommit(new ServerCommit(
                            true,
                            this.serial.incrementAndGet(),
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
                    break;
                }

                // 1) Читаем текущее значение из AtomicReference
                String currentToken = snapshotObject.getToken().get();

                // 2) Сверяем с клиентским по СОДЕРЖИМОМУ
                if (!Objects.equals(currentToken, operationClient.getTokenForUpdate())) {
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "invalid token",
                            null
                    ));
                    break;
                }

                // 3) Пытаемся атомарно заменить ТО, ЧТО ПРОЧИТАЛИ
                String newToken = UUID.randomUUID().toString();
                if (snapshotObject.getToken().compareAndSet(currentToken, newToken)) {
                    resultOperation.setServerCommit(new ServerCommit(
                            true,
                            this.serial.incrementAndGet(),
                            idUser,
                            null,
                            newToken
                    ));
                    snapshotObject.accept(resultOperation);
                    operations.add(resultOperation);
                } else {
                    // между get и CAS кто-то успел обновить
                    String now = snapshotObject.getToken().get();
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "token changed concurrently; expected: '" + currentToken + "', now: '" + now + "'",
                            null
                    ));
                }
            }
        }

        return resultOperation;
    }

    public Map<String, SnapshotObject> getActiveObjects() {
        return objects.entrySet().stream()
                .filter(e -> !e.getValue().isRemove())   // используем геттер
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}