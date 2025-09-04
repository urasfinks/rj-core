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
            @JsonProperty("uuid") String uuid,
            @JsonProperty("timestampAdd") Long timestampAdd,
            @JsonProperty("operationType") OperationType operationType,
            @JsonProperty("tokenForUpdate") String tokenForUpdate,
            @JsonProperty("uuidOperationObject") String uuidOperationObject,
            @JsonProperty("data") Map<String, Object> data
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

    public static OperationClient fromJson(String json) throws JsonProcessingException {
        return UtilJson.objectMapper.readValue(json, OperationClient.class);
    }

    public static OperationClient fromJson(String json, String jsonPtrExpr) throws JsonProcessingException {
        JsonNode root = UtilJson.objectMapper.readTree(json);
        JsonNode node = root.at(jsonPtrExpr);
        return UtilJson.objectMapper.treeToValue(node, OperationClient.class);
    }

}
