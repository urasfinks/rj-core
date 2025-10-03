package ru.jamsys.core.plugin.telegram.structure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Button {

    private final String data;

    private String callback;

    private String url;

    private String webapp;

    public Button(@JsonProperty("data") String data) {
        this.data = data;
    }

}
