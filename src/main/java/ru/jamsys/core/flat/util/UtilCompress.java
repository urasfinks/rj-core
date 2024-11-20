package ru.jamsys.core.flat.util;

import java.io.*;
import java.util.zip.*;

public class UtilCompress {

    public static void gzip(InputStream input, OutputStream output) throws IOException {
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            input.transferTo(gzip);
        }
    }

    public static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gzip(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void unGzip(InputStream input, OutputStream os) throws IOException {
        try (GZIPInputStream is = new GZIPInputStream(input)) {
            is.transferTo(os);
        }
    }

    public static byte[] unGzip(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        unGzip(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void zip(InputStream input, String fileName, OutputStream output) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(output)) {
            zos.putNextEntry(new ZipEntry(fileName));
            input.transferTo(zos);
            zos.closeEntry();
        }
    }

    public static byte[] zip(byte[] input, String fileName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        zip(new ByteArrayInputStream(input), fileName, bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void unZip(InputStream input, String fileName, OutputStream output) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    zis.transferTo(output);
                }
            }
        }
    }

    public static byte[] unZip(byte[] input, String fileName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        unZip(new ByteArrayInputStream(input), fileName, bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void base(InputStream input, OutputStream output) throws IOException {
        try (DeflaterOutputStream dos = new DeflaterOutputStream(output)) {
            input.transferTo(dos);
        }
    }

    public static byte[] base(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        base(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void unBase(InputStream input, OutputStream output) throws IOException {
        try (OutputStream ios = new InflaterOutputStream(output)) {
            input.transferTo(ios);
        }
    }

    public static byte[] unBase(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        unBase(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

}
