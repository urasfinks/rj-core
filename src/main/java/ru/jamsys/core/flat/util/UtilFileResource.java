package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class UtilFileResource {

    public enum Direction {
        WEB,
        CORE,
        PROJECT
    }

    public static InputStream get(String path) throws IOException {
        return get(path, App.springSource.getClassLoader());
    }

    @SuppressWarnings("unused")
    public static boolean isFile(String path, Direction direction) {
        try {
            InputStream inputStream = get(path, direction);
            return inputStream != null;
        } catch (Throwable th) {
            return false;
        }
    }

    public static InputStream get(String path, ClassLoader classLoader) throws IOException {
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new RuntimeException("URL is null (" + path + ")");
        }
        URLConnection conn = resource.openConnection();
        return conn.getInputStream();
    }

    public static InputStream get(String path, Direction direction) throws IOException {
        return switch (direction) {
            case WEB -> new FileInputStream(UtilFile.getWebFile(path));
            case CORE -> get(path, App.class.getClassLoader());
            case PROJECT -> get(path);
        };
    }

    public static String getAsString(String path) throws IOException {
        return getAsString(path, App.springSource.getClassLoader());
    }

    public static String getAsString(String path, ClassLoader classLoader) throws IOException {
        return new String(get(path, classLoader).readAllBytes());
    }

    public static String getAsString(String path, Direction direction) throws IOException {
        return switch (direction) {
            case WEB -> UtilFile.getWebContent(path);
            case CORE -> getAsString(path, App.class.getClassLoader());
            case PROJECT -> getAsString(path);
        };
    }

}
