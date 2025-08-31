package ru.jamsys.core.handler.web.socket.snapshot;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class SnapshotObject {

    private final AtomicReference<String> token;
    private final Map<String, Object> data = new HashMap<>();
    private volatile boolean remove = false;

    public SnapshotObject(String token) {
        this.token = new AtomicReference<>(token);
    }

    public void accept(Operation operation) {
        if (remove) {
            return;
        }
        if (operation.getOperationClient().getOperationType() == OperationType.DELETE) {
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
                .append("token", token.get())
                .append("remove", remove)
                .append("data", data)
                ;
    }

}
