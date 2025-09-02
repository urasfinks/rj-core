package ru.jamsys.core.handler.web.socket.operation;

import lombok.Getter;

import java.util.Map;

@Getter
public class OperationClient {

    private final String uuid;
    private final long timestampAdd;
    private final OperationType operationType;
    private final String tokenForUpdate;
    private final String uuidOperationObject;
    private final Map<String, Object> data;

    public OperationClient(
            String uuid,
            Long timestampAdd,
            OperationType operationType,
            String tokenForUpdate,
            String uuidOperationObject,
            Map<String, Object> data
    ) {
        if (uuid == null) {
            throw new RuntimeException("uuid is null");
        }
        if (timestampAdd == null) {
            throw new RuntimeException("timestampAdd is null");
        }
        if (uuidOperationObject == null) {
            throw new RuntimeException("uuidObject is null");
        }
        this.uuid = uuid;
        this.timestampAdd = timestampAdd;
        this.operationType = operationType;
        this.tokenForUpdate = tokenForUpdate;
        this.uuidOperationObject = uuidOperationObject;
        this.data = data;
    }

}
