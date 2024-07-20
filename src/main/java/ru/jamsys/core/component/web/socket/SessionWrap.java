package ru.jamsys.core.component.web.socket;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.List;

@Data
public class SessionWrap {
    WebSocketSession session;
    UserSessionInfo userSessionInfo;

    String remoteAddress;

    List<String> listSubscribeUuidData = new ArrayList<>();

    public void subscribe(String uuidData) {
        if (!listSubscribeUuidData.contains(uuidData)) {
            listSubscribeUuidData.add(uuidData);
        }
        Util.logConsole("subscribe(" + session.getId() + "); Subscribe list: " + listSubscribeUuidData);
    }

    public void unsubscribe(String uuidData) {
        listSubscribeUuidData.remove(uuidData);
        Util.logConsole("unsubscribe(" + session.getId() + "); Subscribe list: " + listSubscribeUuidData);
    }

    public boolean isSubscribed(String uuidData) {
        return listSubscribeUuidData.contains(uuidData);
    }

    public SessionWrap(WebSocketSession session, UserSessionInfo userSessionInfo, String remoteAddress) {
        this.session = session;
        this.userSessionInfo = userSessionInfo;
        this.remoteAddress = remoteAddress;
    }

}
