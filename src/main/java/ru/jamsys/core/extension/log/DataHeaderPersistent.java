package ru.jamsys.core.extension.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"header", "data"})
public class DataHeaderPersistent extends DataHeader implements DataPersistent {

    public DataHeaderPersistent() {
    }

    public DataHeaderPersistent(Object body) {
        setBody(body);
    }

    public Object getRawBody() {
        return getBody();
    }

    @Override
    public String getBodyString() {
        return String.valueOf(getBody());
    }

    @Override
    public void print() {
        UtilLog.printInfo(new HashMapBuilder<String, Object>()
                .append(
                        "header",
                        new HashMapBuilder<>(header)
                                .append("time", UtilDate.msFormat(getTimeAdd()))
                )
                .append("body", getBody())
        );
    }

    public DataHeaderPersistent addHeader(String key, Object value) {
        addHeader(key, String.valueOf(value));
        return this;
    }

    public DataHeaderPersistent addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public byte[] toBytes() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) header.size())); // В следующие 2 байта записали кол-во заголовков

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilFileByteReader.writeShortString(os, key);
            UtilFileByteReader.writeShortString(os, String.valueOf(header.get(key)));
        }
        // Запись тела
        UtilFileByteReader.writeString(os, getBodyString());
        return os.toByteArray();
    }

    @SuppressWarnings("unused")
    public static DataHeaderPersistent instanceFromBytes(byte[] bytes) throws Exception {
        DataHeaderPersistent dataHeaderPersistent = new DataHeaderPersistent();
        dataHeaderPersistent.fromBytes(bytes);
        return dataHeaderPersistent;
    }

    @Override
    public void fromBytes(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilFileByteReader.readShortString(fis), UtilFileByteReader.readShortString(fis));
        }
        setBody(UtilFileByteReader.readString(fis));
    }

}