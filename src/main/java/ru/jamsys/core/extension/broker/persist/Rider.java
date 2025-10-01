package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.*;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public class Rider extends AbstractManagerElement {

    private ManagerConfiguration<AsyncFileWriterWal<BlockControl>> controlWriterConfiguration;

    private final String ns;

    private final String managerKey;

    private String filePathData;

    private String filePathControl;

    private QueueRetry queueRetry;

    private BrokerPersistRepositoryProperty repositoryProperty;

    private Boolean fileBinFinishState;

    private Consumer<Rider> onWrite;

    // Экземпляр создаётся в onSwap и в commit
    public Rider(String ns, String managerKey) {
        this.ns = ns;
        this.managerKey = managerKey;
    }

    public void setup(
            String filePathData,
            BrokerPersistRepositoryProperty repositoryProperty,
            Consumer<Rider> onWrite,
            boolean fileDataFinishState
    ){
        this.filePathData = filePathData;
        this.filePathControl = BrokerPersist.filePathDataToControl(filePathData);
        this.repositoryProperty = repositoryProperty;
        this.fileBinFinishState = fileDataFinishState;
        this.onWrite = onWrite;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("filePath", filePathControl)
                .append("queueRetry", queueRetry)
                .append("yWriterConfiguration", controlWriterConfiguration)
                ;
    }

    // Когда коммитят Data, мы запускаем запись коммита, а после записи - по data.position удаляем из queueRetry
    // что бы этот Data больше никому не выпал на обработку
    public void onCommitData(Position data) {
        if (queueRetry.isProcessed()) {
            throw new RuntimeException(filePathControl + " queue is empty");
        }
        controlWriterConfiguration.get().writeAsync(new BlockControl(data));
    }

    // Вызывается, когда записалась пачка Data на файловую систему, нам надо разместить её в queueRetry, что бы потом
    // кому-нибудь выдать этот Data на обработку
    public <T extends Position & ByteSerializable> void onWriteData(T data) {
        try {
            queueRetry.add(data.getPosition(), null, data);
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public void runOperation() {
        if (repositoryProperty == null) {
            throw new RuntimeException("repositoryProperty is null; filePath: " + filePathControl);
        }
        if (fileBinFinishState == null) {
            throw new RuntimeException("fileXFinishState is null; filePath: " + filePathControl);
        }
        queueRetry = new QueueRetry(ns, fileBinFinishState);
        // То, что будут коммитить - это значит, что обработано и нам надо это удалять из списка на обработку
        // В asyncWrite залетает CommitElement содержащий bin (CommitElement.getBytes() возвращает позицию bin.position)
        // В onWrite залетает список CommitElement и мы должны bin.position удалить из binReader
        controlWriterConfiguration = ManagerConfiguration.getInstance(
                filePathControl,
                java.util.UUID.randomUUID().toString(),
                AsyncFileWriterWal.class,
                managerElement -> {
                    managerElement.setupRepositoryProperty(repositoryProperty);
                    managerElement.setupOnWrite((_, listY) -> {
                        for (BlockControl blockControl : listY) {
                            queueRetry.remove(blockControl.getData().getPosition());
                        }
                        if (onWrite != null) {
                            onWrite.accept(this);
                        }
                    });
                    managerElement.getListShutdownBefore().add(this);
                }
        );
        // Просто всегда считываем данные из файла. Может быть прийдётся подтюнячить, что бы восстановление не падало
        // при одновременной записи
        try {
            AbstractAsyncFileReader.read(BrokerPersist.filePathControlToData(filePathControl), queueRetry);
            SimpleDataReader controlFileReaderResult = new SimpleDataReader();
            AbstractAsyncFileReader.read(filePathControl, controlFileReaderResult);
            controlFileReaderResult.getQueue().forEach((dataReadWrite) -> {
                long dataPosition = UtilByte.bytesToLong(dataReadWrite.getBytes());
                queueRetry.remove(dataPosition);
            });
        } catch (Throwable th) {
            throw new ForwardException(this, th);
        }
    }

    @Override
    public void shutdownOperation() {
        if (queueRetry.isProcessed()) {
            try {
                UtilFile.remove(filePathControl);
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return queueRetry.flushAndGetStatistic(threadRun);
    }

}
