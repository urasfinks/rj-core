package ru.jamsys.core.extension.batch.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;

import java.util.function.Consumer;

// Блок данных для записи на FS с возможностью вызова callback, когда данные буду сохранены на диске

@Getter
@Setter
public class AsyncFileWriterElement extends AbstractAsyncFileWriterElement {

    @JsonIgnore
    private final byte[] bytes; // Данные которые надо записать

    private long position; // Смещение байт от начала файла, где записаны данные

    private Consumer<AsyncFileWriterElement> onWrite;

    private String filePath;

    public AsyncFileWriterElement(byte[] bytes) {
        this.bytes = bytes;
    }

    public AsyncFileWriterElement(byte[] bytes, Consumer<AsyncFileWriterElement> onWrite) {
        this.bytes = bytes;
        this.onWrite = onWrite;
    }

    public void callback() {
        if (onWrite == null) {
            return;
        }
        try {
            onWrite.accept(this);
        } catch (Throwable th) {
            App.error(th);
        }
    }

}
