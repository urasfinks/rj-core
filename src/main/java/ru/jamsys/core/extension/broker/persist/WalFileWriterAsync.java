package ru.jamsys.core.extension.broker.persist;

// Синхронная запись .wal файла оказалась провальной - около 50к IOPS
// В целом я принял позицию, что мы будем работать в режиме гарантированной доставки, а это значит, что могут быть
// дубликаты. Асинхронная запись может породить только дубликаты, так как если не успеет записаться блок коммита
// произойдёт повторная выдача данных, но это укладывается в механизм гарантированной доставки

import ru.jamsys.core.extension.LifeCycleInterface;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class WalFileWriterAsync implements LifeCycleInterface {

    AtomicBoolean run = new AtomicBoolean(false);

    private final BatchFileWriter batchFileWriter;

    public WalFileWriterAsync(String filePath) throws IOException {
        this.batchFileWriter = new BatchFileWriter(filePath);
    }

    private void restore(){

    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        if (run.compareAndSet(false, true)) {
            restore();
        }
    }

    @Override
    public void shutdown() {
        if (run.compareAndSet(true, false)) {

        }
    }

}
