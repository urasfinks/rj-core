package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.CamelNormalization;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UtilFileResource {

    public enum Direction implements CamelNormalization {

        WEB,
        CORE,
        PROJECT;

        public static Direction valueOfCamel(String key) {
            for (Direction dir : Direction.values()) {
                if (dir.getNameCamel().equals(key)) {
                    return dir;
                }
            }
            return null;
        }

    }

    public static InputStream get(String path) throws IOException {
        return get(path, ClassLoader.getSystemClassLoader());
    }

    public static InputStream get(String path, ClassLoader classLoader) throws IOException {
        return classLoader.getResourceAsStream(path);
    }

    public static InputStream get(String path, Direction direction) throws IOException {
        return switch (direction) {
            case WEB -> new FileInputStream(UtilFile.getWebFile(path));
            case CORE -> get(path, App.class.getClassLoader());
            case PROJECT -> get(path);
        };
    }

    public static String getAsString(String path) throws IOException {
        return getAsString(path, ClassLoader.getSystemClassLoader());
    }

    public static String getAsString(String path, ClassLoader classLoader) throws IOException {
        try (InputStream is = classLoader.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("InputStream is null");
            }
            return new String(is.readAllBytes());
        }
    }

    public static String getAsString(String path, Direction direction) throws IOException {
        return switch (direction) {
            case WEB -> UtilFile.getWebContent(path);
            case CORE -> getAsString(path, App.class.getClassLoader());
            case PROJECT -> getAsString(path);
        };
    }

}
