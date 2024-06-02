package ru.jamsys.core.component.manager.item;

import lombok.ToString;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@ToString
public class Log implements ByteItem {

    public Map<String, String> header = new HashMap<>();
    public String data;

    public Log setData(String data) {
        this.data = data;
        return this;
    }

    public Log addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public byte[] getByteInstance() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) header.size()));

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilLog.shortWriteString(os, key);
            UtilLog.shortWriteString(os, header.get(key));
        }
        // Запись тела
        UtilLog.writeString(os, data);
        return os.toByteArray();
    }

    @Override
    public void instanceFromByte(InputStream fis) throws Exception {
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilLog.shortReadString(fis), UtilLog.shortReadString(fis));
        }
        setData(UtilLog.readString(fis));
    }

}
