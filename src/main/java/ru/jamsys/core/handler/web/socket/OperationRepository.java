package ru.jamsys.core.handler.web.socket;

import lombok.Getter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.handler.web.socket.operation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public class OperationRepository {

    private final Map<String, OperationObject> operationObjects = new ConcurrentHashMap<>();
    private final AtomicInteger serial = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();
    LinkedHashSet<String> serialSet = new LinkedHashSet<>();

    public Operation accept(OperationClient operationClient, String idUser) {
        Operation resultOperation = new Operation(operationClient);
        OperationObject operationObject = operationObjects.computeIfAbsent(
                operationClient.getUuidOperationObject(),
                uuid -> new OperationObject(uuid, uuid)
        );
        String newToken = UUID.randomUUID().toString();
        if (Util.casByEquals(
                operationObject.getRefToken(),
                operationClient.getTokenForUpdate(),
                newToken
        )) {
            resultOperation.setServerCommit(new ServerCommit(
                    true,
                    this.serial.incrementAndGet(),
                    idUser,
                    newToken,
                    null
            ));
            operationObject.accept(resultOperation);
            operations.add(resultOperation);
            serialSet.add(resultOperation.getOperationClient().getUuidOperationObject());
            if (operationClient.getOperationType().equals(OperationType.REMOVE)) {
                serialSet.remove(resultOperation.getOperationClient().getUuidOperationObject());
            }
        } else {
            resultOperation.setServerCommit(new ServerCommit(
                    false,
                    -1,
                    idUser,
                    null,
                    operationObject
            ));
        }
        return resultOperation;
    }

    // Это когда из базы восстанавливаем, что бы в operation не добавлялось
    public void restore(Operation operation) {
        OperationObject operationObject = operationObjects.computeIfAbsent(
                operation.getOperationClient().getUuidOperationObject(),
                uuid -> new OperationObject(uuid, uuid)
        );
        operationObject.accept(operation);
        serialSet.add(operation.getOperationClient().getUuidOperationObject());
    }

    // Возвращает последовательность добавления объектов
    public List<String> getActiveObjectsKeySerial() {
        return new ArrayList<>(serialSet);
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