package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileReader;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterWal;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public class CommitController extends AbstractManagerElement {

    private final Manager.Configuration<AsyncFileWriterWal<CommitElement>> commitControllerConfiguration;

    private final String filePathCommit;

    private final AbstractAsyncFileReader.FileReaderResult binFileReaderResult = new AbstractAsyncFileReader.FileReaderResult();

    // Экземпляр создаётся в onSwap и в commit
    public CommitController(
            ApplicationContext applicationContext,
            String ns,
            String filePathCommit,
            Consumer<CommitController> onWrite
    ) throws IOException {
        this.filePathCommit = filePathCommit;
        // То, что будут коммитить - это значит, что обработано и нам надо это удалять из списка на обработку
        // В asyncWrite залетает CommitElement содержащий bin (CommitElement.getBytes() возвращает позицию bin.position)
        // В onWrite залетает список CommitElement и мы должны bin.position удалить из binReader
        commitControllerConfiguration = App.get(Manager.class).configureGeneric(
                AbstractAsyncFileWriter.class,
                ns,
                ns1 -> new AsyncFileWriterWal<>(
                        applicationContext,
                        ns1,
                        filePathCommit,
                        listCommitElement -> {
                            markActive();
                            listCommitElement.forEach(commitElement -> binFileReaderResult
                                    .remove(commitElement.getBin().getPosition())
                            );
                            onWrite.accept(this);
                        }
                )
        );
    }

    public <T extends ByteSerialization> void asyncWrite(BrokerPersistElement<T> element) {
        markActive();
        commitControllerConfiguration.get().writeAsync(new CommitElement(element));
    }

    public <T extends ByteSerialization> void add(List<BrokerPersistElement<T>> brokerPersistElements) {
        markActive();
        brokerPersistElements.forEach(brokerPersistElement -> {
            try {
                binFileReaderResult.add(brokerPersistElement.getPosition(), null, brokerPersistElement);
            } catch (Exception e) {
                App.error(e);
            }
        });
    }

    // Если мы наткнулись на -1 в основном файле и все position закоммичены
    public boolean isComplete() {
        return binFileReaderResult.isFinishState() && binFileReaderResult.getSize().get() == 0;
    }

    @Override
    public void runOperation() {
        // Просто всегда считываем данные из файла. Может быть прийдётся подтюнячить, что бы восстановление не падало
        // при одновременной записи
        try {
            AbstractAsyncFileReader.read(BrokerPersist.commitToBin(filePathCommit), binFileReaderResult);
            AbstractAsyncFileReader.FileReaderResult commitFileReaderResult = new AbstractAsyncFileReader.FileReaderResult();
            AbstractAsyncFileReader.read(filePathCommit, commitFileReaderResult);
            commitFileReaderResult.getMapData().forEach((_, dataPayload) -> {
                long binPosition = UtilByte.bytesToLong(dataPayload.getBytes());
                binFileReaderResult.remove(binPosition);
            });
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    @Override
    public void shutdownOperation() {
        if (isComplete()) {
            try {
                UtilFile.remove(filePathCommit);
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
