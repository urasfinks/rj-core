package ru.jamsys.core.resource.filebyte.reader;

import lombok.Getter;
import ru.jamsys.core.extension.ByteTransformer;

@Getter
public class FileByteReaderRequest {

    private final String filePath;

    private final Class<? extends ByteTransformer> clsItem;

    public FileByteReaderRequest(String filePath, Class<? extends ByteTransformer> clsItem) {
        this.filePath = filePath;
        this.clsItem = clsItem;
    }
}
