package ru.jamsys.core.handler.web.socket.operation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class OperationObject {

    private final String uuid;
    private final AtomicReference<String> refToken;
    private final Map<String, Object> data = new HashMap<>();
    private volatile boolean remove = false;

    public OperationObject(String uuid, String token) {
        this.uuid = uuid;
        this.refToken = new AtomicReference<>(token);
    }

    public void accept(Operation operation) {
        if (remove) {
            return;
        }
        if (operation.getOperationClient().getOperationType() == OperationType.REMOVE) {
            remove = true;
            return;
        }
        Map<String, Object> clientData = operation.getOperationClient().getData();
        if (clientData != null) {
            data.putAll(clientData);
        } else {
            data.clear();
        }
    }

    // Планируется, что данные вычисляются на основе операций
    @JsonValue
    public Map<String, Object> getData() {
        return new HashMapBuilder<String, Object>()
                .append("token", refToken.get())
                .append("remove", remove)
                .append("data", data)
                ;
    }

}
