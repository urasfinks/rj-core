package ru.jamsys;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@SuppressWarnings("unused")
public class UtilFile {

    @SuppressWarnings("unused")
    public static byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public static void writeBytes(String pathTarget, byte[] data, FileWriteOptions fileWriteOptions) throws IOException {
        Path path = Paths.get(pathTarget);
        Path parent = path.getParent();
        if(parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        switch (fileWriteOptions) {
            case CREATE_OR_REPLACE:
                Files.write(Paths.get(pathTarget), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                break;
            case CREATE_OR_APPEND:
                Files.write(Paths.get(pathTarget), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                break;
            default:
                Files.write(Paths.get(pathTarget), data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }

    @SuppressWarnings("unused")
    public static void remove(String path) throws IOException {
        Files.delete(Paths.get(path));
    }

}
