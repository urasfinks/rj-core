package ru.jamsys.core.flat.util;


import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UtilFile {

    @SuppressWarnings("unused")
    public static byte[] readBytes(String path, byte[] def) {
        try {
            return readBytes(path);
        } catch (Exception ignored) {
        }
        return def;
    }

    public static byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public static void writeBytes(String pathTarget, byte[] data, FileWriteOptions fileWriteOptions) throws IOException {
        Path path = Paths.get(pathTarget);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        switch (fileWriteOptions) {
            case CREATE_OR_REPLACE ->
                    Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            case CREATE_OR_APPEND ->
                    Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            default -> Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }


    public static boolean ifExist(String path) {
        return Files.exists(Paths.get(path));
    }

    public static void removeIfExist(String path) {
        try {
            if (ifExist(path)) {
                remove(path);
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    public static void remove(String path) throws IOException {
        Files.delete(Paths.get(path));
    }

    public static void removeAllFilesInFolder(String path) {
        getFilesRecursive(path).forEach(curPath -> {
            try {
                remove(curPath);
            } catch (IOException e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        });

    }

    public static void listFilesForFolder(final File folder, List<String> list) {
        if (folder != null) {
            for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry, list);
                } else {
                    list.add(fileEntry.getAbsolutePath());
                }
            }
        }
    }

    public static List<String> getFilesRecursive(String path) {
        List<String> result = new ArrayList<>();
        listFilesForFolder(new File(path), result);
        return result;
    }

    public static String getFileName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    public static boolean rename(String pathFrom, String pathTo) {
        try {
            File file = new File(pathFrom);
            File file2 = new File(pathTo);
            return file.renameTo(file2);
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return false;
    }

}
