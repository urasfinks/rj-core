package ru.jamsys.core.handler.web.socket.snapshot;

import lombok.Getter;

import java.util.Map;

@Getter
public class OperationClient {

    private final String uuid;
    private final long timestampAdd;
    private final OperationType operationType;
    private final String tokenForUpdate;
    private final String uuidObject;
    private final Map<String, Object> data;

    public OperationClient(
            String uuid,
            Long timestampAdd,
            OperationType operationType,
            String tokenForUpdate,
            String uuidObject,
            Map<String, Object> data
    ) {
        if (uuid == null) {
            throw new RuntimeException("uuid is null");
        }
        if (timestampAdd == null) {
            throw new RuntimeException("timestampAdd is null");
        }
        if (uuidObject == null) {
            throw new RuntimeException("uuidObject is null");
        }
        this.uuid = uuid;
        this.timestampAdd = timestampAdd;
        this.operationType = operationType;
        this.tokenForUpdate = tokenForUpdate;
        this.uuidObject = uuidObject;
        this.data = data;
    }

}
