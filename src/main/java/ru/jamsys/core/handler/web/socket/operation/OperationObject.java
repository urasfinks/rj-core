package ru.jamsys.core.handler.web.socket.operation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class OperationObject {

    @Getter
    private final String uuid;
    private final AtomicReference<String> refToken;
    private final Map<String, Object> data = new HashMap<>();

    @Getter
    private volatile boolean remove = false;

    public OperationObject(String uuid, String token) {
        this.uuid = uuid;
        this.refToken = new AtomicReference<>(token);
    }

    @Getter
    public static class ResultAccept {

        private final boolean accepted;
        private final String newToken;
        private final String description;

        public ResultAccept(boolean accepted, String newToken, String description) {
            this.accepted = accepted;
            this.newToken = newToken;
            this.description = description;
        }

    }

    public ResultAccept accept(String tokenForUpdate, OperationType operationType, Map<String, Object> newData) {
        if (remove) {
            return new ResultAccept(false, null, "Object already remove");
        }
        String newToken = UUID.randomUUID().toString();
        if (Util.casByEquals(refToken, tokenForUpdate, newToken)) {
            if (operationType == OperationType.REMOVE) {
                remove = true;
                return new ResultAccept(true, newToken, null);
            }
            if (newData != null) {
                data.putAll(newData);
            } else {
                data.clear();
            }
            return new ResultAccept(true, newToken, null);
        }
        return new ResultAccept(false, null, "Invalid token");
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
