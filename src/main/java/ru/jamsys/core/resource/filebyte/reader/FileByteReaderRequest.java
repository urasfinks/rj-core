package ru.jamsys.core.resource.filebyte.reader;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerialization;

@Getter
public class FileByteReaderRequest {

    private final String filePath;

    private final Class<? extends ByteSerialization> clsItem;

    public FileByteReaderRequest(String filePath, Class<? extends ByteSerialization> clsItem) {
        this.filePath = filePath;
        this.clsItem = clsItem;
    }

}
