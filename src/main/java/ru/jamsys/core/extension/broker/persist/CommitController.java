package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterWal;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class CommitController extends AbstractManagerElement {

    private final Manager.Configuration<AsyncFileWriterWal<CommitElement>> asyncFileWriterWalConfiguration;

    private final ConcurrentHashMap<Long, Boolean> uncommitedPosition = new ConcurrentHashMap<>(); // Доступные позиции

    private final AtomicInteger remainingPosition = new AtomicInteger(0); // Счётчик оставшихся позиций

    private final String filePathCommit;

    private final AtomicBoolean finishState = new AtomicBoolean(false); // Встретили -1 длину данных в bin

    // Экземпляр создаётся в onSwap и в commit
    public CommitController(
            ApplicationContext applicationContext,
            String ns,
            String filePathCommit,
            Consumer<CommitController> onWrite
    ) throws IOException {
        this.filePathCommit = filePathCommit;
        asyncFileWriterWalConfiguration = App.get(Manager.class).configureGeneric(
                AbstractAsyncFileWriter.class,
                ns,
                ns1 -> new AsyncFileWriterWal<>(
                        applicationContext,
                        ns1,
                        filePathCommit,
                        list -> {
                            markActive();
                            list.forEach(commitElement -> remove(commitElement.getKey()));
                            onWrite.accept(this);
                        }
                )
        );
    }

    public void fileFinishWrite() {
        finishState.set(true);
    }

    private void addPosition(Long position) {
        // При одновременном чтении файла коммитов и накидывании из брокера
        // могут появится дубли, поэтому computeIfAbsent
        uncommitedPosition.computeIfAbsent(position, _ -> {
            remainingPosition.incrementAndGet();
            return false;
        });

    }

    public <T extends ByteSerialization> void addPosition(List<BrokerPersistElement<T>> brokerPersistElements) {
        markActive();
        for (BrokerPersistElement<T> element : brokerPersistElements) {
            addPosition(element.getPosition());
        }
    }

    private void remove(long position) {
        if (uncommitedPosition.remove(position) != null) {
            remainingPosition.decrementAndGet();
        }
    }

    // Может одновременно вестись запись в этот файл
    public void restorePositionFromFile() throws IOException {
        String filePathBin = BrokerPersist.commitToBin(filePathCommit);
        try (RandomAccessFile file = new RandomAccessFile(filePathBin, "r")) {
            long currentPosition = 0;

            while (true) {
                byte[] lengthBytes = new byte[4]; // Читаем длину следующего блока (4 байта)
                if (file.read(lengthBytes) < 4) {
                    // EOF в процессе записи — файл может быть пуст или частично записан
                    break;
                }

                int length = UtilByte.bytesToInt(lengthBytes);

                if (length == -1) {
                    // Маркер конца файла
                    finishState.set(true);
                    break;
                }

                if (length < 0) {
                    App.error(new IOException("Invalid block in file '" + getFilePathCommit() + "' length encountered: " + length));
                    break;
                }

                long nextPosition = file.getFilePointer() + length;

                if (file.skipBytes(length) != length) {
                    App.error(new IOException("Unexpected end of file '" + getFilePathCommit() + "' while skipping content"));
                    break;
                }

                // Только если блок считан корректно — добавляем позицию
                addPosition(currentPosition);
                currentPosition = nextPosition;
            }
        }
    }

    public void asyncWrite(long position) throws Throwable {
        markActive();
        asyncFileWriterWalConfiguration.get().writeAsync(new CommitElement(position));
    }

    // Если мы наткнулись на -1 в основном файле и все position закоммичены
    public boolean isComplete() {
        return finishState.get() && remainingPosition.get() == 0;
    }

    @Override
    public void runOperation() {
        // Просто всегда считываем данные из файла. Может быть прийдётся подтюнячить, что бы восстановление не падало
        // при одновременной записи
        try {
            restorePositionFromFile();
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
