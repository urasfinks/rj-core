package ru.jamsys.core.flat.util;


import org.apache.tomcat.util.http.fileupload.FileUtils;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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

    @SuppressWarnings("unused")
    public static void writeBytes(String pathTarget, InputStream data) throws IOException {
        Path path = Paths.get(pathTarget);
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        java.nio.file.Files.copy(data, path, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String getRelativePath(String relativePath, String absolutePath) {
        String abs = Paths.get(relativePath).toAbsolutePath().toString();
        return absolutePath.substring(abs.length() - relativePath.length());
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
            App.error(e);
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
                App.error(e);
            }
        });
    }

    // Одно и тоже
    public static void cleanDirectory(String path) throws IOException {
        FileUtils.cleanDirectory(new File(path));
    }

    public static void listFilesForFolder(final File folder, List<String> list) {
        if (folder != null) {
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (final File fileEntry : files) {
                        if (fileEntry.isDirectory()) {
                            listFilesForFolder(fileEntry, list);
                        } else {
                            list.add(fileEntry.getAbsolutePath());
                        }
                    }
                }
            } else {
                throw new RuntimeException("Path [" + folder.getAbsolutePath() + "] is not directory");
            }
        } else {
            throw new RuntimeException("File is null");
        }
    }

    public static List<String> getFilesRecursive(String path) {
        return getFilesRecursive(path, true);
    }

    public static List<String> getFilesRecursive(String path, boolean absolutePath) {
        List<String> result = new ArrayList<>();
        File file = new File(path);
        listFilesForFolder(file, result);
        if (absolutePath) {
            return UtilListSort.sort(result, UtilListSort.Type.ASC);
        } else {
            List<String> result2 = new ArrayList<>();
            int offset = file.getAbsolutePath().length();
            for (String p : result) {
                result2.add(p.substring(offset));
            }
            return UtilListSort.sort(result2, UtilListSort.Type.ASC);
        }
    }

    @SuppressWarnings("all")
    public static boolean rename(String pathFrom, String pathTo) {
        try {
            File file = new File(pathFrom);
            File file2 = new File(pathTo);
            return file.renameTo(file2);
        } catch (Exception e) {
            App.error(e);
        }
        return false;
    }

    public static String getWebFileAsString(String relativeWebPath) {
        String location = App.get(ServiceProperty.class)
                .computeIfAbsent("run.args.web.resource.location", "")
                .get();
        try {
            byte[] bytes = UtilFile.readBytes(location + relativeWebPath);
            return new String(bytes);
        } catch (Throwable th) {
            throw new ForwardException(new HashMapBuilder<>()
                    .append("location", location)
                    .append("relativeWebPath", relativeWebPath),
                    th);
        }
    }

    public static File getWebFile(String relativeWebPath) {
        String location = App.get(ServiceProperty.class)
                .computeIfAbsent("run.args.web.resource.location", "")
                .get();
        return new File(location + relativeWebPath);
    }

    @SuppressWarnings("unused")
    public static void deleteDirectory(File dir) throws IOException {
        FileUtils.deleteDirectory(dir);
    }

    @SuppressWarnings("all")
    public static boolean createNewFile(String filePath) throws IOException {
        File file = new File(filePath);
        return file.createNewFile();
    }

    public static void copyAllFilesRecursive(String sourceDirPath, String targetDirPath) throws IOException {
        Path sourceDir = Paths.get(sourceDirPath);
        Path targetDir = Paths.get(targetDirPath);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Исходная директория не существует или не является директорией: " + sourceDirPath);
        }

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = targetDir.resolve(sourceDir.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, targetDir.resolve(sourceDir.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
