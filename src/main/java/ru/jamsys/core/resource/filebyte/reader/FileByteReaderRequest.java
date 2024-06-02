package ru.jamsys.core.resource.filebyte.reader;

import lombok.Getter;
import ru.jamsys.core.extension.ByteItem;

@Getter
public class FileByteReaderRequest {

    private final String filePath;

    private Class<? extends ByteItem> clsItem;

    public FileByteReaderRequest(String filePath, Class<? extends ByteItem> clsItem) {
        this.filePath = filePath;
        this.clsItem = clsItem;
    }
}
