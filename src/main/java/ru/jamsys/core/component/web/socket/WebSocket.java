package ru.jamsys.core.component.web.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.Util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocket extends TextWebSocketHandler {

    public static String nameSocketRequestReader = "SocketRequestReader";

    public static Map<WebSocketSession, SessionWrap> mapConnection = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setBody(message.getPayload());
        requestMessage.setSessionWrap(mapConnection.get(session));
        //App.context.getBean(Broker.class).add(RequestMessage.class, requestMessage);
        //App.context.getBean(ThreadBalancerFactory.class).getThreadBalancer(nameSocketRequestReader).wakeUp();
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress != null) {
            String host = session.getRemoteAddress().getHostString();
            if (!checkCountConnectionPerHost(host)) {
                Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Overload host connection: " + session);
                close(session, CloseStatus.SERVICE_OVERLOAD);
                return;
            }
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.startsWith("/socket") && path.length() == 44) { //Ждём uuid устройства
                    Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Success: " + session);
                    UserSessionInfo userSessionInfo = new UserSessionInfo();
                    userSessionInfo.setDeviceUuid(path.substring(8));
                    mapConnection.put(session, new SessionWrap(session, userSessionInfo, host));
                } else {
                    Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error: " + session);
                    close(session, CloseStatus.POLICY_VIOLATION);
                }
            } else {
                Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error read uri: " + session);
                close(session, CloseStatus.BAD_DATA);
            }
        } else {
            Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error read address: " + session);
            close(session, CloseStatus.BAD_DATA);
        }
    }

    private boolean checkCountConnectionPerHost(String host) {
        int countConnectionPerHost = 0;
        WebSocketSession[] webSocketSessions = mapConnection.keySet().toArray(new WebSocketSession[0]);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            SessionWrap sessionWrap = mapConnection.get(webSocketSession);
            if (sessionWrap != null) {
                if (sessionWrap.getRemoteAddress().equals(host)) {
                    countConnectionPerHost++;
                }
                if (countConnectionPerHost > 12) { //Наверное 12 человек играть можно в codenames
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionClosed: " + session + "; status: " + status);
        mapConnection.remove(session);
    }


    //Отправка по списку uuidDevice либо сокетам, кто явно подписался на uuidData
    public void send(List<String> listUuidDevice, String uuidData, String data) {
        JsonSchema jsonSchema = new JsonSchema();
        JsonSchema.Result validate = jsonSchema.validate(data, "socket.json");
        if (validate.isValidate()) {
            WebSocketSession[] webSocketSessions = mapConnection.keySet().toArray(new WebSocketSession[0]);
            Util.logConsole(listUuidDevice.toString() + "; data: " + data + "; count connection: " + webSocketSessions.length);
            for (WebSocketSession webSocketSession : webSocketSessions) {
                SessionWrap sessionWrap = mapConnection.get(webSocketSession);
                if (
                        sessionWrap != null
                                && (
                                listUuidDevice.contains(sessionWrap.getUserSessionInfo().getDeviceUuid())
                                        || sessionWrap.isSubscribed(uuidData)
                        )
                ) {
                    send(webSocketSession, data);
                }
            }
        } else {
            new RuntimeException(validate.getError()).printStackTrace();
        }
    }

    private void send(@NotNull WebSocketSession session, String data) {
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Exception e) {
            e.printStackTrace();
            close(session, CloseStatus.SERVER_ERROR);
        }
    }

    private void close(@NotNull WebSocketSession session, CloseStatus closeStatus) {
        try {
            session.close(closeStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapConnection.remove(session);
    }
}
