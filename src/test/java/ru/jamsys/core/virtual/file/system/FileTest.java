package ru.jamsys.core.virtual.file.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.component.VirtualFileSystem;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.FileSaverFactory;
import ru.jamsys.core.flat.util.UtilFile;

class FileTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void testMain() {
        File file = new File("/hello/world/1.txt", () -> null);
        Assertions.assertEquals("/hello/world", file.getFolder(), "Директория");
        Assertions.assertEquals("1", file.getFileName(), "Имя файла");
        Assertions.assertEquals("txt", file.getExtension(), "Расширение");
        Assertions.assertEquals("/hello/world/1.txt", file.getAbsolutePath(), "Полный путь");

        file = new File("/1.txt", () -> null);
        Assertions.assertEquals("/1.txt", file.getAbsolutePath(), "Полный путь");

        file = new File("1.txt", () -> null);
        Assertions.assertEquals("/1.txt", file.getAbsolutePath(), "Полный путь");

        file = new File("test/1.txt", () -> null);
        Assertions.assertEquals("/test/1.txt", file.getAbsolutePath(), "Полный путь");
    }

    @Test
    public void testGetString() throws Exception {
        File file = new File("/hello/world/1.txt", FileLoaderFactory.fromString("Hello world", "UTF-8"));
        Assertions.assertEquals("Hello world", file.getString("UTF-8"), "#1");

        file = new File("/hello/world/1.txt", FileLoaderFactory.fromBase64("SGVsbG8gd29ybGQ=", "UTF-8"));
        Assertions.assertEquals("Hello world", file.getString("UTF-8"), "#2");
    }

    @Test
    public void testGetBase64() throws Exception {
        File file = new File("/hello/world/1.txt", FileLoaderFactory.fromString("Hello world", "UTF-8"));
        Assertions.assertEquals("SGVsbG8gd29ybGQ=", file.getBase64(), "#1");
    }

    @Test
    public void testFromFileSystem() throws Exception {
        File file = new File("/hello/world/1.txt", FileLoaderFactory.fromFileSystem("pom.xml"));
        Assertions.assertFalse(file.getString("UTF-8").isEmpty(), "#1");
    }

    @Test
    public void testKeyStore() throws Exception {
        Security security = App.context.getBean(Security.class);
        security.setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        security.loadKeyStorage(password);
        security.add("test", "12345".toCharArray(), password);

        File file = new File("/test.p12", FileLoaderFactory.createKeyStore("one.jks", "test"));
        file.setSaver(FileSaverFactory.writeFile("one.jks"));
        file.setProperty("securityKey", "test");
        UtilFile.remove("one.jks");
        UtilFile.removeIfExist("unit-test.jks");
    }

    @Test
    void testComponent() {
        File file = new File("hello/world/1.txt", FileLoaderFactory.fromString("Hello world", "UTF-8"));
        VirtualFileSystem virtualFileSystem = App.context.getBean(VirtualFileSystem.class);
        virtualFileSystem.add(file);
        File file1 = virtualFileSystem.get("/hello/world/1.txt");

        Assertions.assertEquals(file, file1, "Файлы не равны");

    }
}