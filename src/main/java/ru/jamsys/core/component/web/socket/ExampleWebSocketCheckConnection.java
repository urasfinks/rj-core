package ru.jamsys.core.component.web.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.jamsys.core.extension.annotation.IgnoreClassFinder;

@IgnoreClassFinder
@Component
public class ExampleWebSocketCheckConnection implements WebSocketCheckConnection {

    @Override
    public boolean check(@NotNull WebSocketSession webSocketSession) {
        System.out.println("UUU");
        return true;
    }

}
