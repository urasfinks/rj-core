package ru.jamsys.core.handler.web.socket;

import lombok.Getter;
import ru.jamsys.core.handler.web.socket.operation.Operation;
import ru.jamsys.core.handler.web.socket.operation.OperationClient;
import ru.jamsys.core.handler.web.socket.operation.OperationObject;
import ru.jamsys.core.handler.web.socket.operation.ServerCommit;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public class OperationRepository {

    private final Map<String, OperationObject> operationObjects = new ConcurrentHashMap<>();
    private final AtomicInteger serial = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    public Operation accept(OperationClient operationClient, String idUser) {
        Operation resultOperation = new Operation(operationClient);

        switch (resultOperation.getOperationClient().getOperationType()) {
            case CREATE -> {
                AtomicBoolean created = new AtomicBoolean(false);
                OperationObject operationObject = operationObjects.computeIfAbsent(
                        operationClient.getUuidOperationObject(),
                        uuid -> {
                            created.set(true);
                            return new OperationObject(uuid);
                        }
                );

                if (created.get()) {
                    resultOperation.setServerCommit(new ServerCommit(
                            true,
                            this.serial.incrementAndGet(),
                            idUser,
                            null,
                            operationClient.getUuidOperationObject(),
                            null
                    ));
                    operationObject.accept(resultOperation);
                    operations.add(resultOperation);
                } else {
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "duplicate " + operationClient.getUuidOperationObject(),
                            null,
                            operationObject
                    ));
                }
            }

            case UPDATE, DELETE -> {
                OperationObject operationObject = operationObjects.get(operationClient.getUuidOperationObject());
                if (operationObject == null) {
                    resultOperation.setServerCommit(new ServerCommit(
                            false,
                            -1,
                            idUser,
                            "not found " + operationClient.getUuidOperationObject(),
                            null,
                            null
                    ));
                    break;
                }
                cas(operationObject, operationClient.getTokenForUpdate(), resultOperation, idUser);
            }
        }

        return resultOperation;
    }

    private void cas(OperationObject operationObject, String tokenForUpdate, Operation resultOperation, String idUser) {
        // 1) Читаем текущее значение из AtomicReference
        String currentToken = operationObject.getToken().get();

        // 2) Сверяем с клиентским по СОДЕРЖИМОМУ
        if (!Objects.equals(currentToken, tokenForUpdate)) {
            resultOperation.setServerCommit(new ServerCommit(
                    false,
                    -1,
                    idUser,
                    "invalid token",
                    null,
                    null
            ));
            return;
        }

        // 3) Пытаемся атомарно заменить ТО, ЧТО ПРОЧИТАЛИ
        String newToken = UUID.randomUUID().toString();
        if (operationObject.getToken().compareAndSet(currentToken, newToken)) {
            resultOperation.setServerCommit(new ServerCommit(
                    true,
                    this.serial.incrementAndGet(),
                    idUser,
                    null,
                    newToken,
                    null
            ));
            operationObject.accept(resultOperation);
            operations.add(resultOperation);
        } else {
            // между get и CAS кто-то успел обновить
            String now = operationObject.getToken().get();
            resultOperation.setServerCommit(new ServerCommit(
                    false,
                    -1,
                    idUser,
                    "token changed concurrently; expected: '" + currentToken + "', now: '" + now + "'",
                    null,
                    null
            ));
        }
    }

    public Map<String, OperationObject> getActiveObjects() {
        return operationObjects.entrySet().stream()
                .filter(e -> !e.getValue().isRemove())   // используем геттер
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}