package ru.jamsys.util;

import org.springframework.util.ResourceUtils;

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

}
