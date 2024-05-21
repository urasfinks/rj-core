package ru.jamsys.core.resource.virtual.file.system;


import ru.jamsys.core.extension.ConsumerThrowing;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;

@SuppressWarnings("unused")
public class FileSaverFactory {

    public static ConsumerThrowing<byte[]> writeFile(String path){
        return (data)-> UtilFile.writeBytes(path, data, FileWriteOptions.CREATE_OR_REPLACE);
    }

}
