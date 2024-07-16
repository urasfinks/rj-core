package ru.jamsys.core.component.manager.item;

import lombok.ToString;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@ToString
public class LogHeader implements ByteTransformer {

    public Map<String, String> header = new HashMap<>();

    public String data;

    public final LogType logType;

    public LogHeader(LogType logType) {
        this.logType = logType;
    }

    public LogHeader setData(String data) {
        this.data = data;
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
            UtilLog.writeShortString(os, key);
            UtilLog.writeShortString(os, header.get(key));
        }
        // Запись тела
        UtilLog.writeString(os, data);
        return os.toByteArray();
    }

    @Override
    public void instanceFromByte(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilLog.readShortString(fis), UtilLog.readShortString(fis));
        }
        setData(UtilLog.readString(fis));
    }

}