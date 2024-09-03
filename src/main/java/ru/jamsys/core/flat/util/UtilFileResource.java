package ru.jamsys.core.flat.util;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import ru.jamsys.core.extension.exception.ForwardException;

import java.io.*;
import java.util.stream.Collectors;

public class UtilFileResource {

    public static InputStream get(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public static String getAsString(String fileName) throws IOException {
        //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = ResourceUtils.getFile("classpath:" + fileName);
        try (InputStream is = new FileInputStream(file)) {
            //try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    public static String getAsStringAny(String path) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        try {
            Reader reader = new InputStreamReader(resource.getInputStream());
            return FileCopyUtils.copyToString(reader);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    public static String getAsStringByClassLoader(ClassLoader classLoader, String path) throws IOException {
        try (InputStream is = classLoader.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("InputStream is null");
            }
            return new String(is.readAllBytes());
        }
    }

}
