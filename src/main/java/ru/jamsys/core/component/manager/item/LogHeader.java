package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLogConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
public class LogHeader implements Log {

    public long timeAdd = System.currentTimeMillis();

    public Map<String, String> header = new LinkedHashMap<>();

    public String data;

    public final LogType logType;

    public LogHeader(LogType logType, Class<?> cls, Object data) {
        this.logType = logType;
        if (data != null) {
            this.data = data instanceof String ? data.toString() : UtilJson.toStringPretty(data, "--");
        }
        addHeader("time", timeAdd);
        addHeader("type", logType.getNameCamel());
        addHeader("thread", Thread.currentThread().getName());
        addHeader("class", cls.getName());
    }

    public LogHeader addHeader(String key, Object value) {
        addHeader(key, String.valueOf(value));
        return this;
    }

    public LogHeader addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public byte[] getByteInstance() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) header.size()));

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilLogConverter.writeShortString(os, key);
            UtilLogConverter.writeShortString(os, header.get(key));
        }
        // Запись тела
        UtilLogConverter.writeString(os, data);
        return os.toByteArray();
    }

    @Override
    public void instanceFromByte(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilLogConverter.readShortString(fis), UtilLogConverter.readShortString(fis));
        }
        setData(UtilLogConverter.readString(fis));
    }

    @Override
    public String getView() {
        header.put("time", UtilDate.msFormat(timeAdd));
        if (data != null) {
            return UtilJson.toString(header, "--") + "\r\n" + data;
        } else {
            return UtilJson.toString(header, "--");
        }
    }

}