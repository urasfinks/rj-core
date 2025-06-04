package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.*;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.log.StatDataHeader;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public class Rider extends AbstractManagerElement {

    private ManagerConfiguration<AsyncFileWriterWal<Y>> yWriterConfiguration;

    private final String filePathX;

    private final String filePathY;

    private QueueRetry queueRetry;

    private BrokerPersistRepositoryProperty repositoryProperty;

    private Boolean fileXFinishState;

    private Consumer<Rider> onWrite;

    // Экземпляр создаётся в onSwap и в commit
    public Rider(String filePathX) {
        this.filePathX = filePathX;
        this.filePathY = BrokerPersist.filePathXToY(filePathX);
    }

    public void setup(
            BrokerPersistRepositoryProperty repositoryProperty,
            Consumer<Rider> onWrite,
            boolean fileXFinishState
    ){
        this.repositoryProperty = repositoryProperty;
        this.fileXFinishState = fileXFinishState;
        this.onWrite = onWrite;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("filePath", filePathY)
                .append("queueRetry", queueRetry)
                .append("yWriterConfiguration", yWriterConfiguration)
                ;
    }

    // Когда коммитят X, мы запускаем запись каммита, а после записи - по x.position удаляем из queueRetry
    // что бы этот X больше никому не выпал на обработку
    public void onCommitX(Position x) {
        if (queueRetry.isProcessed()) {
            throw new RuntimeException(filePathY + " queue is empty");
        }
        yWriterConfiguration.get().writeAsync(new Y(x));
    }

    // Вызывается, когда записалась пачка X на файловую систему, нам надо разместить её в queueRetry, что бы потом
    // кому-нибудь выдать этот X на обработку
    public <T extends Position & ByteSerializable> void onWriteX(T x) {
        try {
            queueRetry.add(x.getPosition(), null, x);
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public void runOperation() {
        if (repositoryProperty == null) {
            throw new RuntimeException("repositoryProperty is null; filePath: " + filePathY);
        }
        if (fileXFinishState == null) {
            throw new RuntimeException("fileXFinishState is null; filePath: " + filePathY);
        }
        queueRetry = new QueueRetry(filePathX, fileXFinishState);
        // То, что будут коммитить - это значит, что обработано и нам надо это удалять из списка на обработку
        // В asyncWrite залетает CommitElement содержащий bin (CommitElement.getBytes() возвращает позицию bin.position)
        // В onWrite залетает список CommitElement и мы должны bin.position удалить из binReader
        yWriterConfiguration = ManagerConfiguration.getInstance(
                AsyncFileWriterWal.class,
                java.util.UUID.randomUUID().toString(),
                filePathY,
                managerElement -> {
                    managerElement.setupRepositoryProperty(repositoryProperty);
                    managerElement.setupOnWrite((_, listY) -> {
                        for (Y y : listY) {
                            queueRetry.remove(y.getX().getPosition());
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
            AbstractAsyncFileReader.read(BrokerPersist.filePathYToX(filePathY), queueRetry);
            SimpleDataReader yFileReaderResult = new SimpleDataReader();
            AbstractAsyncFileReader.read(filePathY, yFileReaderResult);
            yFileReaderResult.getQueue().forEach((dataReadWrite) -> {
                long xPosition = UtilByte.bytesToLong(dataReadWrite.getBytes());
                queueRetry.remove(xPosition);
            });
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    @Override
    public void shutdownOperation() {
        if (queueRetry.isProcessed()) {
            try {
                UtilFile.remove(filePathY);
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    @Override
    public List<StatDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return queueRetry.flushAndGetStatistic(threadRun);
    }

}
