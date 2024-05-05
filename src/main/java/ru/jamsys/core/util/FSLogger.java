package ru.jamsys.core.util;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.api.BrokerManager;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class FSLogger {

    @ToString
    public static class Item {
        public Map<String, String> header;
        public String data;

        public Item(Map<String, String> header, String data) {
            this.header = header;
            this.data = data;
        }
    }

    public final Broker<Item> toFs;

    public final Broker<Item> fromFs;

    public FSLogger(ApplicationContext applicationContext) {
        @SuppressWarnings("unchecked")
        BrokerManager<Item> broker = applicationContext.getBean(BrokerManager.class);
        toFs = broker.get(getClass().getSimpleName() + ".toFS");
        fromFs = broker.get(getClass().getSimpleName() + ".fromFS");
    }

    public Item append(Map<String, String> header, String data) throws Exception {
        if (header.size() < Short.MAX_VALUE) {
            Item item = new Item(header, data);
            toFs.add(item, 6_000L);
            return item;
        }
        return null;
    }

    public void write() {
        try (FileOutputStream fos = new FileOutputStream("data.bin")) {
            while (!toFs.isEmpty()) {
                TimeEnvelopeMs<Item> itemTimeEnvelopeMs = toFs.pollFirst();
                Item item = itemTimeEnvelopeMs.getValue();
                // Запись кол-ва заголовков
                fos.write(shortToBytes((short) item.header.size()));
                // Запись заголовков
                for (String key : item.header.keySet()) {
                    writeShortStringBlock(fos, key);
                    writeShortStringBlock(fos, item.header.get(key));
                }
                // Запись тела
                writeStringBlock(fos, item.data);
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    public void read() {
        try (FileInputStream fis = new FileInputStream("data.bin")) {
            while (fis.available() > 0) {
                Item item = new Item(new HashMap<>(), "");
                short countHeader = bytesToShort(fis.readNBytes(2));
                for (int i = 0; i < countHeader; i++) {
                    item.header.put(readShortString(fis), readShortString(fis));
                }
                item.data = readString(fis);
                fromFs.add(item, 6_000L);
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    public String readShortString(FileInputStream fis) throws Exception {
        short len = bytesToShort(fis.readNBytes(2));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public String readString(FileInputStream fis) throws Exception {
        int len = bytesToInt(fis.readNBytes(4));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public void writeShortStringBlock(FileOutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(shortToBytes((short) dataBytes.length));
        fos.write(dataBytes);
    }

    public void writeStringBlock(FileOutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(intToBytes(dataBytes.length));
        fos.write(dataBytes);
    }


    public static byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF));
    }

    public static byte[] shortToBytes(short s) {
        return ByteBuffer.allocate(2).putShort(s).array();
    }

    public static short bytesToShort(byte[] bytes) {
        //System.out.println("bytesToShort("+Arrays.toString(bytes)+")");
        return ByteBuffer.wrap(bytes).getShort();
    }

}
