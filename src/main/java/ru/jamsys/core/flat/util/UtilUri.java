package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UtilUri {

    public static String buildUrlQuery(String path, Map<String, ?> getParameters) {
        List<String> part = new ArrayList<>();
        if (!getParameters.isEmpty()) {
            getParameters.forEach((key, element) -> {
                if (element instanceof List) {
                    @SuppressWarnings("all")
                    List<Object> list = (List<Object>) element;
                    list.forEach(s1 -> {
                        try {
                            part.add(key + "=" + encode(String.valueOf(s1)));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    try {
                        part.add(key + "=" + encode(String.valueOf(element)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return path + (
                getParameters.isEmpty()
                        ? ""
                        : "?" + String.join("&", part)
        );
    }

    @JsonPropertyOrder({"path", "folder", "fileName", "extension", "uri"})
    @Getter
    public static class FilePath {

        private final String path;          // Оригинальный путь
        private final String folder;        // Директория от пути
        private final String fileName;      // Имя файла
        private final String extension;     // Расширение файла
        private final String parameters;    // Параметры

        public FilePath(String path) {
            // ? это делитель параметров, прежде всего делим path на uri
            if (path.contains("?")) {
                int i = path.indexOf("?");
                this.parameters = path.substring(i + 1);
                path = path.substring(0, i);
            } else {
                this.parameters = null;
            }
            // Вырезка ..
            List<String> newItems = new ArrayList<>();
            for (String p : path.trim().split("/")) {
                if (p != null) {
                    if (p.equals("..")) {
                        if (!newItems.isEmpty()) {
                            newItems.removeLast();
                        } else {
                            throw new RuntimeException("Exception remove .. from path: " + path);
                        }
                    } else {
                        newItems.add(p);
                    }
                }
            }
            StringBuilder newPath = new StringBuilder();
            newPath.append(String.join("/", newItems));
            if (parameters != null) {
                newPath.append("?").append(parameters);
            }
            this.path = newPath.toString();
            this.fileName = newItems.removeLast();
            if (!newItems.isEmpty()) {
                this.folder = String.join("/", newItems);
            } else {
                this.folder = null;
            }
            String[] split = this.fileName.split("\\.");
            if (split.length > 1) {
                this.extension = split[split.length - 1].trim();
            } else {
                this.extension = null;
            }
        }

    }

    public static FilePath parsePath(String uri) {
        return new FilePath(uri);
    }

    public static String encode(String data, String charset) throws Exception {
        return URLEncoder.encode(data, charset);
    }

    public static String encode(String data) throws Exception {
        return encode(data, Util.defaultCharset);
    }

    public static String decode(String data, String charset) throws Exception {
        return URLDecoder.decode(data, charset);
    }

    public static String decode(String data) throws Exception {
        return decode(data, Util.defaultCharset);
    }

}
