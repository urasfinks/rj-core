package ru.jamsys.core.resource.virtual.file.system;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.SupplierThrowing;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;

@SuppressWarnings("unused")
public class ReadFromSourceFactory {

    public static SupplierThrowing<byte[]> fromFileSystem(String path) {
        return () -> UtilFile.readBytes(path);
    }

    public static SupplierThrowing<byte[]> fromBase64(String base64decoded, String charset) {
        return () -> UtilBase64.decodeResultBytes(base64decoded, charset);
    }

    public static SupplierThrowing<byte[]> fromString(String data, String charset) {
        return () -> data.getBytes(Charset.forName(charset));
    }

    @SuppressWarnings("unused")
    public static SupplierThrowing<byte[]> createFileAndRead(String path, String data, String charset) {
        try {
            UtilFile.writeBytes(path, data.getBytes(charset), FileWriteOptions.CREATE_OR_REPLACE);
            return fromFileSystem(path);
        } catch (Throwable th) {
            throw new ForwardException(new HashMapBuilder<>()
                    .append("path", path)
                    .append("dataLength", data != null ? data.length() : null)
                    .append("charset", charset), th);
        }
    }

    public static SupplierThrowing<byte[]> createKeyStoreAndRead(String path, String securityAlias) {
        try {
            KeyStore ks = KeyStore.getInstance("JCEKS");
            SecurityComponent securityComponent = App.get(SecurityComponent.class);
            char[] pass = securityComponent.get(securityAlias);
            ks.load(null, pass);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ks.store(byteArrayOutputStream, pass);
            UtilFile.writeBytes(path, byteArrayOutputStream.toByteArray(), FileWriteOptions.CREATE_OR_REPLACE);
            return fromFileSystem(path);
        } catch (Throwable th) {
            throw new ForwardException(new HashMapBuilder<>()
                    .append("path", path)
                    .append("alias", securityAlias),
                    th);
        }
    }

}
