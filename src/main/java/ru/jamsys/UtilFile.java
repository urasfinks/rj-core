package ru.jamsys;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class UtilFile {

    public static byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    public static void writeBytes(String path, byte[] data, FileWriteOptions fileWriteOptions) throws IOException {
        switch (fileWriteOptions) {
            case CREATE_OR_REPLACE:
                Files.write(Paths.get(path), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                break;
            case CREATE_OR_APPEND:
                Files.write(Paths.get(path), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                break;
            default:
                Files.write(Paths.get(path), data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }

    public static void remove(String path) throws IOException {
        Files.delete(Paths.get(path));
    }

}
