package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.functional.iface.ConsumerThrowing;
import ru.jamsys.core.extension.functional.iface.SupplierThrowing;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.resource.virtual.file.system.view.FileView;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class File extends ExpirationMsMutableImpl
        implements
        StatisticsFlush,
        KeepAlive,
        Property<String, Object>,
        LifeCycleInterface,
        ClassEquals
{

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
                FileView fileView = cls.getDeclaredConstructor().newInstance();
                fileView.set(this);
                fileView.createCache();
                return fileView;
            } catch (Throwable th) {
                App.error(th);
            }
            return null;
        });
    }

    public <T extends FileView> T getView(Class<T> cls) {
        init();
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

    private void init() {
        try {
            if (fileData == null) {
                fileData = loader.get();
            }
            active();
        } catch (Throwable th) {
            App.error(th);
        }
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
        view.clear();
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

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public void run() {
        // Пока ничего не надо
    }

    @Override
    public void shutdown() {
        reset();
    }

}
