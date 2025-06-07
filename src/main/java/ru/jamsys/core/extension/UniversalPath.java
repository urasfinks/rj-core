package ru.jamsys.core.extension;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class UniversalPath {

    private final String path;          // Оригинальный путь
    private final String folder;        // Директория от пути
    private final String fileName;      // Имя файла
    private final String extension;     // Расширение файла
    private final String parameters;    // Параметры

    public UniversalPath(String inputPath) {
        if (inputPath == null) {
            throw new RuntimeException("inputPath is null");
        }
        // ? это делитель параметров, прежде всего делим path на uri
        if (inputPath.contains("?")) {
            int i = inputPath.indexOf("?");
            parameters = inputPath.substring(i + 1);
            inputPath = inputPath.substring(0, i);
        } else {
            parameters = null;
        }
        // Вырезка ..
        List<String> newItems = new ArrayList<>();
        for (String p : inputPath.trim().split("/")) {
            if (p.equals("..")) {
                if (!newItems.isEmpty()) {
                    newItems.removeLast();
                } else {
                    throw new RuntimeException("Exception remove .. from path: " + inputPath);
                }
            } else {
                newItems.add(p);
            }
        }

        StringBuilder newPath = new StringBuilder();
        if (newItems.isEmpty() && inputPath.startsWith("/")) {
            newPath.append("/");
        } else {
            newPath.append(String.join("/", newItems));
        }
        path = newPath.toString();
        if (!newItems.isEmpty()) {
            fileName = newItems.removeLast();
            String tmpFolder = String.join("/", newItems);
            folder = tmpFolder.isEmpty() ? null : tmpFolder;
            String[] split = fileName.split("\\.");
            if (split.length > 1) {
                extension = split[split.length - 1].trim();
            } else {
                extension = null;
            }
        } else {
            fileName = null;
            folder = null;
            extension = null;
        }
    }

    public String getUri() {
        StringBuilder newPath = new StringBuilder(path);
        if (parameters != null) {
            newPath.append("?").append(parameters);
        }
        return newPath.toString();
    }

    public String getPathWithoutProtocol() {
        if (path == null || path.isEmpty()) return "";

        String s = path;

        int schemeIndex = s.indexOf("://");
        int pathStart;

        if (schemeIndex >= 0) {
            // Пропускаем схему, ищем первый слеш после неё
            pathStart = s.indexOf("/", schemeIndex + 3);
            if (pathStart == -1) return "/"; // нет слеша = корень
            return s.substring(pathStart);
        }

        // Если нет схемы — не трогаем строку, возвращаем как есть
        return s;
    }

    public Map<String, String> parseParameter() {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        return UtilUri.parseParameters(
                "?" + parameters,
                strings -> String.join(",", strings)
        );
    }

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("path", path)
                .append("folder", folder)
                .append("fileName", fileName)
                .append("extension", extension)
                .append("parameters", parameters);
    }

}
