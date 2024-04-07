package ru.jamsys.virtual.file.system;


import ru.jamsys.extension.ConsumerThrowing;
import ru.jamsys.util.FileWriteOptions;
import ru.jamsys.util.UtilFile;

@SuppressWarnings("unused")
public class FileSaverFactory {

    public static ConsumerThrowing<byte[]> writeFile(String path){
        return (data)-> UtilFile.writeBytes(path, data, FileWriteOptions.CREATE_OR_REPLACE);
    }

}
