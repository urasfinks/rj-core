package ru.jamsys.core.handler.web.socket.operation;

import lombok.Getter;

@Getter
public class ServerCommit {

    private final long timestampAdd;
    private final boolean commit;
    private final int id;
    private final String idUser;
    private final String cause;
    private final String newTokenForUpdate;

    public ServerCommit(boolean commit, int id, String idUser, String cause, String newTokenForUpdate) {
        this.timestampAdd = System.currentTimeMillis();
        this.commit = commit;
        this.id = id;
        this.idUser = idUser;
        this.cause = cause;
        this.newTokenForUpdate = newTokenForUpdate;
    }

}
