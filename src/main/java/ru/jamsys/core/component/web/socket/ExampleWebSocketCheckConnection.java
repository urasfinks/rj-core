package ru.jamsys.core.component.web.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.jamsys.core.extension.annotation.ServiceClassFinderIgnore;
import ru.jamsys.core.flat.util.UtilLog;

@SuppressWarnings("unused")
@ServiceClassFinderIgnore
@Component
public class ExampleWebSocketCheckConnection implements WebSocketCheckConnection {

    @Override
    public boolean check(@NotNull WebSocketSession webSocketSession) {
        UtilLog.printInfo(getClass(), "Hello");
        return true;
    }

}
