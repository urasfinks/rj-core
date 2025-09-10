package ru.jamsys.core.component.web.socket;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RouteGenerator;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.extension.RouteGeneratorRepository;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.validate.ValidateType;
import ru.jamsys.core.handler.web.socket.WebSocketHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Lazy
@Component
public class WebSocket extends TextWebSocketHandler implements StatisticsFlushComponent {

    private final WebSocketCheckConnection webSocketCheckConnection;
    private final Map<String, List<WebSocketSession>> subscription = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> connections = Util.getConcurrentHashSet();
    private final RouteGeneratorRepository routeGeneratorRepository;

    public WebSocket(ServiceClassFinder serviceClassFinder, RouteGenerator routeGenerator) {
        List<Class<WebSocketCheckConnection>> byInstance = serviceClassFinder.findByInstance(WebSocketCheckConnection.class);
        if (byInstance.isEmpty()) {
            throw new RuntimeException("WebSocket not found WebSocketCheckConnection component");
        }
        webSocketCheckConnection = serviceClassFinder.instanceOf(byInstance.getFirst());
        routeGeneratorRepository = routeGenerator.getRouterRepository(WebSocketHandler.class);
    }

    @SuppressWarnings("unused")
    public void subscribe(String key, WebSocketSession webSocketSession) {
        subscription.computeIfAbsent(key, _ -> new ArrayList<>()).add(webSocketSession);
    }

    @SuppressWarnings("unused")
    public void unsubscribe(String key, WebSocketSession webSocketSession) {
        if (
                subscription.getOrDefault(key, List.of()).remove(webSocketSession)
                        && subscription.get(key).isEmpty()
        ) {
            subscription.remove(key);
        }
    }

    @SuppressWarnings("unused")
    public void unsubscribeAll(WebSocketSession webSocketSession) {
        UtilRisc.forEach(null, subscription, (s, webSocketSessions) -> {
            webSocketSessions.remove(webSocketSession);
        });
    }

    @SuppressWarnings("unused")
    public void notify(String key, String data) {
        UtilRisc.forEach(null, subscription.getOrDefault(key, List.of()), webSocketSession -> {
            send(webSocketSession, data);
        });
    }

    private void close(@NotNull WebSocketSession session, CloseStatus closeStatus) {
        try {
            session.close(closeStatus);
        } catch (Throwable th) {
            App.error(th);
        }
        remove(session);
    }

    private void remove(@NotNull WebSocketSession webSocketSession) {
        connections.remove(webSocketSession);
        UtilRisc.forEach(null, subscription, (_, webSocketSessions) -> {
            webSocketSessions.remove(webSocketSession);
        });
    }

    public void send(@NotNull WebSocketSession session, String data) {
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Throwable th) {
            App.error(th);
            close(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession webSocketSession, @NotNull CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(webSocketSession, closeStatus);
        remove(webSocketSession);
    }

    @Getter
    @Setter
    public static class Request {
        private final WebSocketSession webSocketSession;
        private final Map<String, Object> request;

        public Request(WebSocketSession webSocketSession, Map<String, Object> request) {
            this.webSocketSession = webSocketSession;
            this.request = request;
        }

    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String request = message.getPayload();
        // Что бы маршрутизировать сообщение в генератор, надо получить uri маршрутизации, поэтому валидация
        ValidateType.JSON.validate(
                request,
                UtilFileResource.getAsString("schema/web/socket/ProtocolRequest.json"),
                null
        );
        Map<String, Object> req;
        try {
            req = UtilJson.getMapOrThrow(request);
        } catch (Throwable th) {
            throw new ForwardException(message, th);
        }
        PromiseGeneratorExternalRequest promiseGenerator = routeGeneratorRepository.match((String) req.get("uri"));
        if (promiseGenerator == null) {
            App.error(new RuntimeException("PromiseGenerator not found"), req);
            return;
        }
        Promise promise = promiseGenerator.generate();
        if (promise == null) {
            App.error(new RuntimeException("Promise is null"));
            return;
        }
        promise.setRepositoryMapClass(Request.class, new Request(session, req));
        promise.run();
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        if (webSocketCheckConnection == null || !webSocketCheckConnection.check(session)) {
            close(session, CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatisticDataHeader> result = new ArrayList<>();
        result.add(new StatisticDataHeader(getClass(), null)
                .addHeader("connections", connections.size())
                .addHeader("subscription", subscription.size())
        );
        return result;
    }

}
