package ru.jamsys.core.component.manager.item;

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
public class LogSimple implements Log {

    public long timeAdd = System.currentTimeMillis();

    public LogType logType;

    public String data;

    public LogSimple(LogType logType) {
        this.logType = logType;
    }

    public LogSimple setData(String data) {
        this.data = data;
        return this;
    }

    public byte[] getByteInstance() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        UtilLogConverter.writeShortString(os, logType.getNameCamel());
        UtilLogConverter.writeShortString(os, timeAdd + "");
        UtilLogConverter.writeString(os, data);
        return os.toByteArray();
    }

    @Override
    public void instanceFromByte(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        logType = LogType.valueOf(UtilCodeStyle.camelToSnake(UtilLogConverter.readShortString(fis)));
        timeAdd = Long.parseLong(UtilLogConverter.readShortString(fis));
        setData(UtilLogConverter.readString(fis));
    }

    @Override
    public String getView() {
        return "";
    }

    @Override
    public Map<String, String> getHeader() {
        return Map.of();
    }

}
