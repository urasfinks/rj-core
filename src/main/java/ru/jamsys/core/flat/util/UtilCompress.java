package ru.jamsys.core.flat.util;

import java.io.*;
import java.util.zip.*;

public class UtilCompress {

    public static void compressGzip(InputStream input, OutputStream output) throws IOException {
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            input.transferTo(gzip);
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
            is.transferTo(os);
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
            input.transferTo(zos);
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
                    zis.transferTo(output);
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
            input.transferTo(dos);
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
            input.transferTo(ios);
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
