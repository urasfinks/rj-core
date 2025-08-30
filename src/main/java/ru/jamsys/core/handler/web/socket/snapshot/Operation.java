package ru.jamsys.core.handler.web.socket.snapshot;

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

}
