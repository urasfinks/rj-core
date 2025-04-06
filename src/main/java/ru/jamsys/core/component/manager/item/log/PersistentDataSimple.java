package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.UtilCodeStyle;
import ru.jamsys.core.flat.util.UtilFileByteReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"subscriberStatusRead", "logType", "timeAdd", "header", "data"})
public class PersistentDataSimple implements PersistentData {

    private short subscriberStatusRead;

    public long timeAdd = System.currentTimeMillis();

    public LogType logType;

    public String body;

    public PersistentDataSimple() {}

    public PersistentDataSimple setBody(String body) {
        this.body = body;
        return this;
    }

    public String getBody() {
        return String.valueOf(body);
    }

    public byte[] toByte() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        UtilFileByteReader.writeShortString(os, logType.getNameCamel());
        UtilFileByteReader.writeShortString(os, timeAdd + "");
        UtilFileByteReader.writeString(os, body);
        return os.toByteArray();
    }

    public static PersistentDataSimple instanceFromBytes(byte[] bytes, short subscriberStatusRead) throws Exception {
        PersistentDataSimple persistentDataSimple = new PersistentDataSimple();
        persistentDataSimple.toObject(bytes);
        persistentDataSimple.setSubscriberStatusRead(subscriberStatusRead);
        return persistentDataSimple;
    }

    @Override
    public void toObject(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        logType = LogType.valueOf(UtilCodeStyle.camelToSnake(UtilFileByteReader.readShortString(fis)));
        timeAdd = Long.parseLong(UtilFileByteReader.readShortString(fis));
        setBody(UtilFileByteReader.readString(fis));
    }

    @JsonIgnore
    @Override
    public String getView() {
        return "";
    }

    @Override
    public Map<String, String> getHeader() {
        return Map.of();
    }

}
