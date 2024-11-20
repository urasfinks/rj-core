package ru.jamsys.core.flat.util;

import java.io.*;
import java.util.zip.*;

public class UtilCompress {

    private static void swap(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int totalSize;
        while ((totalSize = input.read(buffer)) > 0) {
            output.write(buffer, 0, totalSize);
        }
    }

    public static void compressGzip(InputStream input, OutputStream output) throws IOException {
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            swap(input, gzip);
        }
    }

    public static byte[] compressGzip(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        compressGzip(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void decompressGzip(InputStream input, OutputStream os) throws IOException {
        try (GZIPInputStream is = new GZIPInputStream(input)) {
            swap(is, os);
        }
    }

    public static byte[] decompressGzip(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        decompressGzip(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void compressZip(InputStream input, String fileName, OutputStream output) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(output)) {
            zos.putNextEntry(new ZipEntry(fileName));
            swap(input, zos);
            zos.closeEntry();
        }
    }

    public static byte[] compressZip(byte[] input, String fileName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        compressZip(new ByteArrayInputStream(input), fileName, bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void decompressZip(InputStream input, String fileName, OutputStream output) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    swap(zis, output);
                }
            }
        }
    }

    public static byte[] decompressZip(byte[] input, String fileName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        decompressZip(new ByteArrayInputStream(input), fileName, bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void compressBase(InputStream input, OutputStream output) throws IOException {
        try (DeflaterOutputStream dos = new DeflaterOutputStream(output)) {
            swap(input, dos);
        }
    }

    public static byte[] compressBase(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        compressBase(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

    public static void decompressBase(InputStream input, OutputStream output) throws IOException {
        try (OutputStream ios = new InflaterOutputStream(output)) {
            swap(input, ios);
        }
    }

    public static byte[] decompressBase(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        decompressBase(new ByteArrayInputStream(input), bos);
        byte[] response = bos.toByteArray();
        bos.close();
        return response;
    }

}
