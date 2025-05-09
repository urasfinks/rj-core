package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterWal;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class CommitController extends AbstractManagerElement {

    private final Manager.Configuration<AsyncFileWriterWal<CommitElement>> asyncFileWriterWalConfiguration;

    private final ConcurrentHashMap<Long, Boolean> availablePosition = new ConcurrentHashMap<>(); // Доступные позиции

    private final AtomicInteger remainingPosition = new AtomicInteger(0); // Счётчик оставшихся позиций

    private final String filePathWal;

    private final String filePathOrigin;

    private final AtomicBoolean finishState = new AtomicBoolean(false);

    public CommitController(ApplicationContext applicationContext, String ns, String filePathOrigin) throws IOException {
        this.filePathOrigin = filePathOrigin;
        this.filePathWal = filePathOrigin + ".wal";
        asyncFileWriterWalConfiguration = App.get(Manager.class).configureGeneric(
                AsyncFileWriterWal.class,
                ns,
                ns1 -> new AsyncFileWriterWal<>(
                        applicationContext,
                        ns1,
                        filePathWal,
                        list -> list.forEach(commitElement -> remove(commitElement.getKey()))
                )
        );
    }

    public void fileFinishWrite() {
        finishState.set(true);
    }

    private void addPosition(Long position) {
        availablePosition.put(position, false);
        remainingPosition.incrementAndGet();
    }

    public <T extends ByteSerialization> void addPosition(List<BrokerPersistElement<T>> brokerPersistElements) {
        for (BrokerPersistElement<T> element : brokerPersistElements) {
            addPosition(element.getPosition());
        }
    }

    private void remove(long position) {
        if (availablePosition.remove(position) != null) {
            remainingPosition.decrementAndGet();
        }
    }

    //  Восстановить данные с файловой системы
    public void restorePositionFromFile() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePathOrigin, "r")) {
            long currentPosition = 0;

            while (true) {
                addPosition(currentPosition);
                byte[] lengthBytes = new byte[4]; // Читаем длину следующего блока (4 байта)
                int bytesRead = file.read(lengthBytes);
                if (bytesRead < 4) { // Проверяем, достигнут ли EOF (ещё пишется)
                    // EOF в процессе записи, спокойно выходим из цикла
                    break;
                }
                if (file.read(lengthBytes) != 4) {
                    throw new IOException("Unexpected end of file while reading length");
                }
                int length = UtilByte.bytesToInt(lengthBytes);
                if (length == -1) { // Проверяем маркер конца файла
                    finishState.set(true);
                    break;
                }
                if (length < 0) { // Проверка на валидность блока
                    throw new IOException("Invalid block length encountered: " + length);
                }
                currentPosition = file.getFilePointer() + length; // Перемещаем указатель на следующий блок

                // Пропускаем содержимое текущего блока
                if (file.skipBytes(length) != length) {
                    throw new IOException("Unexpected end of file while skipping content");
                }
            }
        }
    }

    public void asyncWrite(long position) throws Throwable {
        asyncFileWriterWalConfiguration.get().writeAsync(new CommitElement(position));
    }

    // Если мы наткнулись на -1 в основном файле и все position закомичены
    public boolean isComplete() {
        return finishState.get() && remainingPosition.get() == 0;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {
        if (isComplete()) {
            try {
                UtilFile.remove(filePathWal);
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
