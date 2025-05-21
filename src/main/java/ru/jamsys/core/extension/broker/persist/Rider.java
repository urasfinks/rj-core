package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.*;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public class Rider extends AbstractManagerElement {

    private final Manager.Configuration<AsyncFileWriterWal<Y>> yWriterConfiguration;

    private final String filePathY;

    private final QueueRetry queueRetry;

    // Экземпляр создаётся в onSwap и в commit
    public Rider(
            BrokerPersistRepositoryProperty repositoryProperty,
            String ns,
            String filePathX,
            boolean fileXFinishState,
            Consumer<Rider> onWrite
    ) {
        this.filePathY = BrokerPersist.filePathXToY(filePathX);
        queueRetry = new QueueRetry(filePathX, fileXFinishState);
        // То, что будут коммитить - это значит, что обработано и нам надо это удалять из списка на обработку
        // В asyncWrite залетает CommitElement содержащий bin (CommitElement.getBytes() возвращает позицию bin.position)
        // В onWrite залетает список CommitElement и мы должны bin.position удалить из binReader
        yWriterConfiguration = App.get(Manager.class).configureGeneric(
                AbstractAsyncFileWriter.class,
                ns,
                _ -> {
                    AsyncFileWriterWal<Y> asyncFileWriterWal = new AsyncFileWriterWal<>(
                            repositoryProperty,
                            filePathY,
                            (_, listY) -> {
                                markActive();
                                for (Y y : listY) {
                                    queueRetry.remove(y.getX().getPosition());
                                }
                                onWrite.accept(this);
                            }
                    );
                    asyncFileWriterWal.getListShutdownBefore().add(this);
                    return asyncFileWriterWal;
                }
        );
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("filePath", filePathY)
                .append("queueRetry", queueRetry)
                ;
    }

    // Когда коммитят X, мы запускаем запись каммита, а после записи - по x.position удаляем из queueRetry
    // что бы этот X больше никому не выпал на обработку
    public void onCommitX(Position x) {
        if (queueRetry.isProcessed()) {
            throw new RuntimeException(filePathY + " queue is empty");
        }
        markActive();
        yWriterConfiguration.get().writeAsync(new Y(x));
    }

    // Вызывается, когда записалась пачка X на файловую систему, нам надо разместить её в queueRetry, что бы потом
    // кому-нибудь выдать этот X на обработку
    public <T extends Position & ByteSerializable> void onWriteX(T x) {
        markActive();
        try {
            queueRetry.add(x.getPosition(), null, x);
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public void runOperation() {
        // Просто всегда считываем данные из файла. Может быть прийдётся подтюнячить, что бы восстановление не падало
        // при одновременной записи
        try {
            AbstractAsyncFileReader.read(BrokerPersist.filePathYToX(filePathY), queueRetry);
            SimpleDataReader yFileReaderResult = new SimpleDataReader();
            AbstractAsyncFileReader.read(filePathY, yFileReaderResult);
            yFileReaderResult.getQueue().forEach((dataPayload) -> {
                long xPosition = UtilByte.bytesToLong(dataPayload.getBytes());
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
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return queueRetry.flushAndGetStatistic(threadRun);
    }

}
