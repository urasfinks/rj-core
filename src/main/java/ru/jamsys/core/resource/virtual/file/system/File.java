package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.resource.virtual.file.system.view.FileView;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.flat.util.UtilBase64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class File extends ExpirationMsMutableImpl implements Closable, StatisticsFlush, KeepAlive, Property<String> {

    @Getter
    protected String folder; //Абсолютный путь виртуальной папки
    @Getter
    protected String fileName; //Имя файла
    @Getter
    protected String extension; //Расширение файла (думаю сделать поиск по расширениям)

    protected SupplierThrowing<byte[]> loader = () -> null;

    @Setter
    private ConsumerThrowing<byte[]> saver = null;

    private volatile byte[] fileData;

    Map<Class<? extends FileView>, FileView> view = new ConcurrentHashMap<>();

    @Getter
    Map<String, Object> mapProperty = new ConcurrentHashMap<>();

    @Getter
    private String absolutePath = null;

    private <T extends FileView> FileView setView(Class<T> cls) {
        return view.computeIfAbsent(cls, _ -> {
            try {
                FileView e = cls.getDeclaredConstructor().newInstance();
                e.set(this);
                init();
                return e;
            } catch (Throwable ex) {
                App.context.getBean(ExceptionHandler.class).handler(ex);
            }
            return null;
        });
    }

    public <T extends FileView> T getView(Class<T> cls) {
        @SuppressWarnings("unchecked")
        T fileView = (T) setView(cls);
        return fileView;
    }

    public <T extends FileView> T getView(Class<T> cls, Object... props) {
        for (int i = 0; i < props.length; i += 2) {
            setProperty(props[i].toString(), props[i + 1]);
        }
        return getView(cls);
    }

    public File(String path, SupplierThrowing<byte[]> loader) {
        init(path, loader);
    }

    public File(String path, SupplierThrowing<byte[]> loader, int cacheTimeMillis) {
        setKeepAliveOnInactivityMs(cacheTimeMillis);
        init(path, loader);
    }

    private void init(String path, SupplierThrowing<byte[]> loader) {
        this.loader = loader;
        active();
        parsePath(path);
    }

    private void parsePath(String path) {
        ArrayList<String> items = new ArrayList<>(Arrays.asList(path.trim().split("/")));
        while (true) {
            String s = items.getFirst();
            if (s == null || s.isEmpty() || s.equals("..")) {
                items.removeFirst();
            } else {
                break;
            }
        }
        String name = items.removeLast();
        String[] split = name.split("\\.");
        this.extension = split[split.length - 1].trim();
        this.fileName = name.substring(0, name.length() - this.extension.length() - 1);
        this.folder = "/" + String.join("/", items);
        if (folder.equals("/")) {
            this.absolutePath = "/" + fileName + "." + extension;
        } else {
            this.absolutePath = folder + "/" + fileName + "." + extension;
        }
    }

    private void init() throws Exception {
        if (fileData == null) {
            fileData = loader.get();
            Set<Class<? extends FileView>> classes = view.keySet();
            for (Class<? extends FileView> c : classes) {
                view.get(c).createCache();
            }

        }
        active();
    }

    public byte[] getBytes() throws Exception {
        init();
        return fileData;
    }

    public String getString(String charset) throws Exception {
        return new String(getBytes(), charset);
    }

    public InputStream getInputStream() throws Exception {
        return new ByteArrayInputStream(getBytes());
    }

    public String getBase64() throws Exception {
        return UtilBase64.base64Encode(getBytes(), true);
    }

    public void save(byte[] data) throws Exception {
        if (saver == null) {
            throw new Exception("Consumer saver not found. File: " + getAbsolutePath());
        }
        fileData = data;
        saver.accept(data);
        //reload(); //Сохранение не должно вызывать перезагрузку, так как в памяти должны быть внесены изменения, это только для Supplier загрузчика
    }

    public void reset() {
        fileData = null;
    }

    @Override
    public void close() {
        reset();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        return new ArrayList<>();
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (isExpired()) {
            reset();
        }
    }

}
