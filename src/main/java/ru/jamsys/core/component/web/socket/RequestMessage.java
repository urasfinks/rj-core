package ru.jamsys.core.component.web.socket;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestMessage {
    SessionWrap sessionWrap;

    public void setBody(String payload) {

    }
}
