package ru.jamsys.core.extension.broker.persist;

// Синхронная запись .wal файла оказалась провальной - около 50к IOPS
// В целом я принял позицию, что мы будем работать в режиме гарантированной доставки, а это значит, что могут быть
// дубликаты. Асинхронная запись может породить только дубликаты, так как если не успеет записаться блок коммита
// произойдёт повторная выдача данных, но это укладывается в механизм гарантированной доставки

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.batch.writer.BatchFileWriter;

import java.util.concurrent.atomic.AtomicBoolean;

public class WalFileWriterAsync implements LifeCycleInterface {

    @Getter
    private final AtomicBoolean operation = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean run = new AtomicBoolean(false);

    @Getter
    @Setter
    private Thread threadOperation;

    private final BatchFileWriter batchFileWriter;

    public WalFileWriterAsync(String filePath) {
        this.batchFileWriter = new BatchFileWriter(filePath);
    }

    private void restore(){

    }

    @Override
    public void runOperation() {
        restore();
    }

    @Override
    public void shutdownOperation() {

    }

}
