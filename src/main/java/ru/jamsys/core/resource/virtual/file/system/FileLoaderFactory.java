package ru.jamsys.core.resource.virtual.file.system;

import ru.jamsys.core.App;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.extension.SupplierThrowing;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyStore;

@SuppressWarnings("unused")
public class FileLoaderFactory {

    public static SupplierThrowing<byte[]> fromFileSystem(String path) {
        return () -> UtilFile.readBytes(path);
    }

    public static SupplierThrowing<byte[]> fromBase64(String base64decoded, String charset) {
        return () -> UtilBase64.base64DecodeResultBytes(base64decoded, charset);
    }

    public static SupplierThrowing<byte[]> fromString(String data, String charset) {
        return () -> data.getBytes(Charset.forName(charset));
    }

    @SuppressWarnings("unused")
    public static SupplierThrowing<byte[]> createFile(String path, String data, String charset) throws IOException {
        UtilFile.writeBytes(path, data.getBytes(charset), FileWriteOptions.CREATE_OR_REPLACE);
        return fromFileSystem(path);
    }

    public static SupplierThrowing<byte[]> createKeyStore(String path, String securityKey) throws Exception {
        KeyStore ks = KeyStore.getInstance("JCEKS");
        Security security = App.context.getBean(Security.class);
        char[] pass = security.get(securityKey);
        ks.load(null, pass);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ks.store(byteArrayOutputStream, pass);
        UtilFile.writeBytes(path, byteArrayOutputStream.toByteArray(), FileWriteOptions.CREATE_OR_REPLACE);
        return fromFileSystem(path);
    }

}
