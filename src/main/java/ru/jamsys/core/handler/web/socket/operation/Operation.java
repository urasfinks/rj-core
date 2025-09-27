package ru.jamsys.core.handler.web.socket.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Operation {

    private final OperationClient operationClient;

    @Setter
    private ServerCommit serverCommit;

    public Operation(OperationClient operationClient) {
        this.operationClient = operationClient;
    }

    @JsonCreator
    public Operation(
            @JsonProperty("operationClient") OperationClient operationClient,           // из кода
            @JsonProperty("serverCommit") ServerCommit serverCommit    // из JSON
    ) {
        this.operationClient = operationClient;
        this.serverCommit = serverCommit;
    }

}
