package ru.jamsys.core.handler.web.socket.operation;

import lombok.Getter;

@Getter
public class ServerCommit {

    private final long timestampAdd;
    private final boolean commit;
    private final int id;
    private final String idUser;
    private final String newTokenForUpdate;

    // Для случаев, когда происходит дупликация при вставке, мы будем оповещать commit false и возвращать замещающий
    private final OperationObject replaceOperationObject;

    public ServerCommit(
            boolean commit,
            int id,
            String idUser,
            String newTokenForUpdate,
            OperationObject replaceOperationObject
    ) {
        this.timestampAdd = System.currentTimeMillis();
        this.commit = commit;
        this.id = id;
        this.idUser = idUser;
        this.newTokenForUpdate = newTokenForUpdate;
        this.replaceOperationObject = replaceOperationObject;
    }

}
