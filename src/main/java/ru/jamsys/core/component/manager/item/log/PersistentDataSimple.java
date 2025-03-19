package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.UtilCodeStyle;
import ru.jamsys.core.flat.util.UtilLogConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"writerFlag", "logType", "timeAdd", "header", "data"})
public class PersistentDataSimple implements PersistentData {

    private short writerFlag;

    public long timeAdd = System.currentTimeMillis();

    public LogType logType;

    public String body;

    public PersistentDataSimple() {}

    public PersistentDataSimple(LogType logType) {
        this.logType = logType;
    }

    public PersistentDataSimple setBody(String body) {
        this.body = body;
        return this;
    }

    public byte[] toByte() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        UtilLogConverter.writeShortString(os, logType.getNameCamel());
        UtilLogConverter.writeShortString(os, timeAdd + "");
        UtilLogConverter.writeString(os, body);
        return os.toByteArray();
    }

    public static PersistentDataSimple instanceFromBytes(byte[] bytes, short writerFlag) throws Exception {
        PersistentDataSimple persistentDataSimple = new PersistentDataSimple();
        persistentDataSimple.toObject(bytes);
        persistentDataSimple.setWriterFlag(writerFlag);
        return persistentDataSimple;
    }

    @Override
    public void toObject(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        logType = LogType.valueOf(UtilCodeStyle.camelToSnake(UtilLogConverter.readShortString(fis)));
        timeAdd = Long.parseLong(UtilLogConverter.readShortString(fis));
        setBody(UtilLogConverter.readString(fis));
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

    @Override
    public short getWriterFlag() {
        return writerFlag;
    }

    @Override
    public void setWriterFlag(short writerFlag) {
        this.writerFlag = writerFlag;
    }

}
