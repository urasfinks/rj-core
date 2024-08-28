package ru.jamsys.core.component.web.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.core.App;
import ru.jamsys.core.HttpController;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.web.socket.WebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Lazy
@Component
public class WebSocket extends TextWebSocketHandler implements StatisticsFlushComponent {

    private final WebSocketCheckConnection webSocketCheckConnection;

    private final Map<String, List<WebSocketSession>> subscription = new ConcurrentHashMap<>();

    private final Set<WebSocketSession> connections = Util.getConcurrentHashSet();

    private final Map<String, PromiseGenerator> path = new LinkedHashMap<>();

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public WebSocket(ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder) {
        List<Class<WebSocketCheckConnection>> byInstance = serviceClassFinder.findByInstance(WebSocketCheckConnection.class);
        if (byInstance.isEmpty()) {
            throw new RuntimeException("WebSocket not found WebSocketCheckConnection component");
        }
        webSocketCheckConnection = serviceClassFinder.instanceOf(byInstance.getFirst());
        HttpController.fill(path, applicationContext, serviceClassFinder, WebSocketHandler.class);
    }

    public void subscribe(String key, WebSocketSession webSocketSession) {
        subscription.computeIfAbsent(key, _ -> new ArrayList<>()).add(webSocketSession);
    }

    public void unsubscribe(String key, WebSocketSession webSocketSession) {
        if (
                subscription.getOrDefault(key, List.of()).remove(webSocketSession)
                        && subscription.get(key).isEmpty()
        ) {
            subscription.remove(key);
        }
    }

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

    private void send(@NotNull WebSocketSession session, String data) {
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Throwable th) {
            App.error(th);
            close(session, CloseStatus.SERVER_ERROR);
        }
    }

    private PromiseGenerator getGeneratorByHandler(String requestUri) {
        for (String pattern : path.keySet()) {
            if (antPathMatcher.match(pattern, requestUri)) {
                return path.get(pattern);
            }
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession webSocketSession, @NotNull CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(webSocketSession, closeStatus);
        remove(webSocketSession);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String request = message.getPayload();
        JsonSchema.validate(request, UtilFileResource.getAsString("schema/web/socket/ProtocolRequest.json"), null);
        Map<Object, Object> req = UtilJson.toMap(request).getObject();
        PromiseGenerator promiseGenerator = getGeneratorByHandler((String) req.get("uri"));
        if (promiseGenerator == null) {
            App.error(new RuntimeException("PromiseGenerator not found"));
            return;
        }
        Promise promise = promiseGenerator.generate();
        if (promise == null) {
            App.error(new RuntimeException("Promise is null"));
            return;
        }
        promise.setMapRepository("WebSocketSession", session);
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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("connections", connections.size())
                .addField("subscription", subscription.size())
        );
        return result;
    }

}
