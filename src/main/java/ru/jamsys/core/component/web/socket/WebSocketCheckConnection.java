package ru.jamsys.core.component.web.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.WebSocketSession;

public interface WebSocketCheckConnection {
    boolean check(@NotNull WebSocketSession webSocketSession);
}
