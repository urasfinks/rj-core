package ru.jamsys.core.handler.web.socket.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.Map;

@Getter
public class OperationClient {

    private final String uuid;
    private final long timestampAdd;
    private final OperationType operationType;
    private final String tokenForUpdate;
    private final String uuidOperationObject;
    private final Map<String, Object> data;

    @JsonCreator
    public OperationClient(
            @JsonProperty(value = "uuid", required = true) String uuid,
            @JsonProperty(value = "timestampAdd", required = true) Long timestampAdd,
            @JsonProperty(value = "operationType", required = true) OperationType operationType,
            @JsonProperty(value = "tokenForUpdate", required = true) String tokenForUpdate,
            @JsonProperty(value = "uuidOperationObject", required = true) String uuidOperationObject,
            @JsonProperty(value = "data") Map<String, Object> data
    ) {
        if (uuid == null || uuid.isBlank()) {
            throw new RuntimeException("uuid is null");
        }
        if (timestampAdd == null || timestampAdd < 0) {
            throw new RuntimeException("timestampAdd is null");
        }
        if (operationType == null) {
            throw new RuntimeException("operationType is null");
        }
        if (tokenForUpdate == null) {
            throw new RuntimeException("tokenForUpdate is null");
        }
        if (uuidOperationObject == null || uuidOperationObject.isBlank()) {
            throw new RuntimeException("uuidObject is null");
        }

        // Необязательные поля — проверяем при необходимости
        if (tokenForUpdate.length() > 256) {
            throw new IllegalArgumentException("tokenForUpdate is too long");
        }
        if (data != null && data.size() > 50) {
            throw new IllegalArgumentException("data has too many entries");
        }

        this.uuid = uuid;
        this.timestampAdd = timestampAdd;
        this.operationType = operationType;
        this.tokenForUpdate = tokenForUpdate;
        this.uuidOperationObject = uuidOperationObject;
        this.data = data;
    }

    public static OperationClient fromJson(String json) throws JsonProcessingException {
        return UtilJson.objectMapper.readValue(json, OperationClient.class);
    }

    public static OperationClient fromJson(String json, String jsonPtrExpr) throws JsonProcessingException {
        JsonNode root = UtilJson.objectMapper.readTree(json);
        JsonNode node = root.at(jsonPtrExpr);
        return UtilJson.objectMapper.treeToValue(node, OperationClient.class);
    }

    public static OperationClient fromMap(Map<String, Object> map) {
        return UtilJson.objectMapper.convertValue(map, OperationClient.class);
    }

}
