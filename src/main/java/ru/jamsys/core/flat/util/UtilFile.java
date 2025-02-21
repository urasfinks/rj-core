package ru.jamsys.core.flat.util;


import org.apache.tomcat.util.http.fileupload.FileUtils;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
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

    public static String getExtension(String uri) {
        String[] split = uri.split("\\?");
        if (split.length > 0) {
            uri = split[0].trim();
        }
        split = uri.split("/");
        if (split.length > 0) {
            uri = split[split.length - 1].trim();
        }
        split = uri.split("\\\\");
        if (split.length > 0) {
            uri = split[split.length - 1].trim();
        }
        split = uri.split("\\.");
        if (split.length > 0) {
            uri = split[split.length - 1].trim();
        }
        return uri;
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

    public static String getFileName(String path) {
        return Paths.get(path).getFileName().toString();
    }

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

    public static String getWebContent(String relativeWebPath) {
        String location = App.get(ServiceProperty.class)
                .computeIfAbsent("run.args.web.resource.location", "", UtilFile.class.getName())
                .get();
        try {
            byte[] bytes = UtilFile.readBytes(location + relativeWebPath);
            return new String(bytes);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    public static File getWebFile(String relativeWebPath) {
        String location = App.get(ServiceProperty.class)
                .computeIfAbsent("run.args.web.resource.location", "", UtilFile.class.getName())
                .get();
        return new File(location + relativeWebPath);
    }

    public static void removeDir(File dir) throws IOException {
        FileUtils.deleteDirectory(dir);
    }

}
