package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilFileByteReader;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"header", "data"})
public class PersistentDataHeader extends DataHeader implements PersistentData {

    public PersistentDataHeader() {
    }

    public PersistentDataHeader(Class<?> cls, Object body) {
        this.body = body;
        addHeader("time", timeAdd);
        addHeader("thread", Thread.currentThread().getName());
        addHeader("source", cls.getName());
    }

    @Override
    public PersistentDataHeader putAll(Map<String, ?> map) {
        return (PersistentDataHeader) super.putAll(map);
    }

    @Override
    public PersistentDataHeader put(String key, Object value) {
        return (PersistentDataHeader) super.put(key, value);
    }

    public Object getRawBody() {
        return body;
    }

    public String getBody() {
        return String.valueOf(body);
    }

    @Override
    public void print() {
        System.out.println(UtilJson.toStringPretty(
                new HashMapBuilder<String, Object>()
                        .append(
                                "header",
                                new HashMapBuilder<>(header)
                                        .append("time", UtilDate.msFormat(timeAdd))
                        )
                        .append("body", body),
                "--"
        ));
    }

    public PersistentDataHeader addHeader(String key, Object value) {
        addHeader(key, String.valueOf(value));
        return this;
    }

    public PersistentDataHeader addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public byte[] toByte() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) header.size())); // В следующие 2 байта записали кол-во заголовков

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilFileByteReader.writeShortString(os, key);
            UtilFileByteReader.writeShortString(os, String.valueOf(header.get(key)));
        }
        // Запись тела
        UtilFileByteReader.writeString(os, getBody());
        return os.toByteArray();
    }

    public static PersistentDataHeader instanceFromBytes(byte[] bytes) throws Exception {
        PersistentDataHeader persistentDataHeader = new PersistentDataHeader();
        persistentDataHeader.toObject(bytes);
        return persistentDataHeader;
    }

    @Override
    public void toObject(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilFileByteReader.readShortString(fis), UtilFileByteReader.readShortString(fis));
        }
        setBody(UtilFileByteReader.readString(fis));
    }

}