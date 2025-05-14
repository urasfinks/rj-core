package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.batch.writer.*;
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
            ApplicationContext applicationContext,
            String ns,
            String filePathY,
            Consumer<Rider> onWrite
    ) {
        this.filePathY = filePathY;
        queueRetry = new QueueRetry(BrokerPersist.filePathYToX(filePathY));
        // То, что будут коммитить - это значит, что обработано и нам надо это удалять из списка на обработку
        // В asyncWrite залетает CommitElement содержащий bin (CommitElement.getBytes() возвращает позицию bin.position)
        // В onWrite залетает список CommitElement и мы должны bin.position удалить из binReader
        yWriterConfiguration = App.get(Manager.class).configureGeneric(
                AbstractAsyncFileWriter.class,
                ns,
                ns1 -> new AsyncFileWriterWal<>(
                        applicationContext,
                        ns1,
                        filePathY,
                        (_, listY) -> {
                            markActive();
                            for (Y y : listY) {
                                queueRetry.remove(y.getX().getPosition());
                            }
                            onWrite.accept(this);
                        }
                )
        );
    }
    // Когда коммитят X, мы запускаем запись каммита, а после записи - по x.position удаляем из queueRetry
    // что бы этот X больше никому не выпал на обработку
    public void onCommitX(Position x) {
        markActive();
        yWriterConfiguration.get().writeAsync(new Y(x));
    }

    // Вызывается, когда записалась пачка X на файловую систему, нам надо разместить её в queueRetry, что бы потом
    // кому-нибудь выдать этот X на обработку
    public <T extends Position & ByteSerializable> void onWriteX(List<T> listX) {
        markActive();
        for (T x : listX) {
            try {
                queueRetry.add(x.getPosition(), null, x);
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    // Если мы наткнулись на -1 в основном файле и все position закоммичены
    public boolean isComplete() {
        return queueRetry.isFinishState() && queueRetry.isEmpty();
    }

    @Override
    public void runOperation() {
        // Просто всегда считываем данные из файла. Может быть прийдётся подтюнячить, что бы восстановление не падало
        // при одновременной записи
        try {
            AbstractAsyncFileReader.read(BrokerPersist.filePathYToX(filePathY), queueRetry);
            FileReaderResult yFileReaderResult = new FileReaderResult();
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
        if (isComplete()) {
            try {
                UtilFile.remove(filePathY);
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
