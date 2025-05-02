package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.RepositoryMap;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.SupplierThrowing;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilUri;
import ru.jamsys.core.resource.virtual.file.system.view.FileView;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class File extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        StatisticsFlush,
        RepositoryMap<String, Object>,
        ManagerElement
{
    final private UtilUri.FilePath filePath;

    protected SupplierThrowing<byte[]> loader = () -> null;

    @Setter
    private ConsumerThrowing<byte[]> saver = null;

    private volatile byte[] fileData;

    Map<Class<? extends FileView>, FileView> view = new ConcurrentHashMap<>();

    @Getter
    Map<String, Object> repositoryMap = new ConcurrentHashMap<>();

    private <T extends FileView> FileView setView(Class<T> cls) {
        return view.computeIfAbsent(cls, _ -> {
            FileView fileView;
            try {
                fileView = cls.getDeclaredConstructor().newInstance();
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
            fileView.set(this);
            fileView.createCache();
            return fileView;
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
            setRepositoryMap(props[i].toString(), props[i + 1]);
        }
        return getView(cls);
    }

    public File(String path, SupplierThrowing<byte[]> loader) {
        init(path, loader);
        this.filePath = UtilUri.parsePath(path);
    }

    public File(String path, SupplierThrowing<byte[]> loader, int cacheTimeMillis) {
        setKeepAliveOnInactivityMs(cacheTimeMillis);
        init(path, loader);
        this.filePath = UtilUri.parsePath(path);
    }

    private void init(String path, SupplierThrowing<byte[]> loader) {
        this.loader = loader;
        markActive();
    }

    private void init() {
        try {
            if (fileData == null) {
                fileData = loader.get();
            }
            markActive();
        } catch (Throwable th) {
            App.error(th);
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
        return UtilBase64.encode(getBytes(), true);
    }

    public void save(byte[] data) throws Throwable {
        if (saver == null) {
            throw new Exception("Consumer saver not found. File: " + filePath.getPath());
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
    public List<DataHeader> flushAndGetStatistic( AtomicBoolean threadRun) {
        return new ArrayList<>();
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {
        reset();
    }

}
