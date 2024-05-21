package ru.jamsys.core.component.manager;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
@Component
public class LogManager implements ClassName {

    public final ConcurrentHashMap<String, Broker<Log>> map = new ConcurrentHashMap<>();

    private final BrokerManager<Log> brokerManager;

    private final RateLimit rateLimit;

    private final Map<String, AtomicInteger> indexFile = new ConcurrentHashMap<>();

    private final String logFolder;

    public LogManager(
            BrokerManager<Log> brokerManager,
            ApplicationContext applicationContext,
            PropertiesComponent propertiesComponent
    ) {
        this.brokerManager = brokerManager;
        rateLimit = applicationContext.getBean(RateLimitManager.class).get(getClassName(applicationContext))
                .init(RateLimitName.FILE_LOG_SIZE.getName(), RateLimitItemInstance.MAX)
                .init(RateLimitName.FILE_LOG_INDEX.getName(), RateLimitItemInstance.MAX);
        int fileLogSizeMb = propertiesComponent.getProperties("rj.log.manager.file.log.size.mb", Integer.class);
        rateLimit.get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(fileLogSizeMb * 1_024 * 1_024);
        int fileLogIndex = propertiesComponent.getProperties("rj.log.manager.file.log.index", Integer.class);
        rateLimit.get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(fileLogIndex);

        this.logFolder = propertiesComponent.getProperties("rj.log.manager.folder", String.class);
    }

    // В стандартной истории есть циклические очереди по N элементов
    // При наполнении очереди начало очереди будет вылетать
    // Так же сообщения будут вылетать через 6 секунд после вставки
    // Каждую секунду приходит Cron::LogToFs
    // Начинает вычитывать очередь и записывать на FS
    // Запись будет файлы не более 20мб
    // Имя файла: [0-N].indexBroker.start.bin -> по завершению [0-N].indexBroker.stop.bin
    // При старте приклада будет пробежка по FS с расчётом текущего уже записанного индекса
    // Все файлы на момент старта с типом start в 3-ей секции имени файла будут удалены
    // При достижении индекса до N будет происходить перезапись с 0
    public void append(String indexBroker, Log log) throws Exception {
//        map.computeIfAbsent(indexBroker, s -> {
//            Broker<Log> logBroker = brokerManager.get(getClassName(indexBroker));
//            // Если будет больше логов в 1 секунду, чем длина очереди - мы начнём более раннии логи терять
//            // Я пока не знаю, хорошо это или плохо - надо тестировать
//            logBroker.setMaxSizeQueue(5_000);
//            return logBroker;
//        }).add(log, 6_000L);
    }

//    public List<Map<String, Integer>> writeToFs(String indexBroker) {
//        List<Map<String, Integer>> result = new ArrayList<>();
//        Broker<Log> logBroker = brokerManager.get(getClassName(indexBroker));
//        RateLimitItem maxIndex = rateLimit.get(RateLimitName.FILE_LOG_INDEX.getName());
//        int maxCountLoop = (int) rateLimit.get(RateLimitName.FILE_LOG_INDEX.getName()).getMax();
//        int counter = 1;
//        while (!logBroker.isEmpty()) {
//            AtomicInteger atomicInteger = indexFile.computeIfAbsent(indexBroker, _
//                    -> new AtomicInteger(1));
//            if (!maxIndex.check(atomicInteger.get())) {
//                atomicInteger.set(1);
//            }
//            String path = indexBroker + "." + atomicInteger.getAndIncrement();
//            result.add(write(logFolder, path, logBroker));
//            if (counter++ > maxCountLoop) {
//                break;
//            }
//        }
//        return result;
//    }

    private Map<String, Integer> write(String dir, String path, Broker<Log> logBroker) {
        RateLimitItem maxSize = rateLimit.get(RateLimitName.FILE_LOG_SIZE.getName());
        AtomicInteger writeByte = new AtomicInteger();
        Exception exception = null;
        List<ExpirationMsImmutableEnvelope<Log>> insuranceRestore = new ArrayList<>();
        try (FileOutputStream fos = new FileOutputStream(dir + "/" + path + ".start.bin")) {
            while (!logBroker.isEmpty()) {
                ExpirationMsImmutableEnvelope<Log> itemExpirationMsMutableEnvelope = logBroker.pollFirst();
                // Запоминаем что считали, что бы если что-то произойдёт с FS - мы ба восстановили в очереди
                insuranceRestore.add(itemExpirationMsMutableEnvelope);

                Log item = itemExpirationMsMutableEnvelope.getValue();
                // Запись кол-ва заголовков
                writeByte.addAndGet(2);
                fos.write(UtilByte.shortToBytes((short) item.header.size()));
                // Запись заголовков
                for (String key : item.header.keySet()) {
                    writeShortStringBlock(fos, key, writeByte);
                    writeShortStringBlock(fos, item.header.get(key), writeByte);
                }
                // Запись тела
                writeStringBlock(fos, item.data, writeByte);
                if (!maxSize.check(writeByte.get())) {
                    break;
                }
            }
        } catch (Exception e) {
            exception = e;
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        if (exception == null) {
            UtilFile.rename(dir + "/" + path + ".start.bin", dir + "/" + path + ".stop.bin");
            return new HashMapBuilder<String, Integer>().append(dir + "/" + path + ".stop.bin", writeByte.get());
        } else {
            try {
                // Решил объединить блок, если не получится вставить обратно в очередь
                // То и удалять файл не буду
                // Может быть получится хоть что-то восстановить из него, но с другой стороны
                // я хотел удалять файлы .start.bin при старте приложения, так как они поломанные
                // может стоит пересмотреть первичную позицию
                for (ExpirationMsImmutableEnvelope<Log> restore : insuranceRestore) {
                    logBroker.add(restore);
                }
                UtilFile.remove(dir + "/" + path + ".start.bin");
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
            return new HashMapBuilder<>();
        }
    }

    public List<Log> read() {
        List<Log> result = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream("data.bin")) {
            while (fis.available() > 0) {
                Log item = new Log(new HashMap<>(), "");
                short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
                for (int i = 0; i < countHeader; i++) {
                    item.header.put(readShortString(fis), readShortString(fis));
                }
                item.data = readString(fis);
                result.add(item);
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return result;
    }

    public String readShortString(FileInputStream fis) throws Exception {
        short len = UtilByte.bytesToShort(fis.readNBytes(2));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public String readString(FileInputStream fis) throws Exception {
        int len = UtilByte.bytesToInt(fis.readNBytes(4));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public void writeShortStringBlock(FileOutputStream fos, String data, AtomicInteger writeByte) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.shortToBytes((short) dataBytes.length));
        fos.write(dataBytes);
        writeByte.addAndGet(dataBytes.length + 2);
    }

    public void writeStringBlock(FileOutputStream fos, String data, AtomicInteger writeByte) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.intToBytes(dataBytes.length));
        fos.write(dataBytes);
        writeByte.addAndGet(dataBytes.length + 4);
    }

}
