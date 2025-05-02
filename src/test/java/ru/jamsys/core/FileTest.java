package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilUri;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.FileSaverFactory;
import ru.jamsys.core.flat.util.UtilFile;

// IO time: 88ms
// COMPUTE time: 89ms

class FileTest {
    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void testMain() {
        File file = new File("/hello/world/1.txt", () -> null);
        Assertions.assertEquals("/hello/world", file.getFilePath().getFolder(), "Директория");
        Assertions.assertEquals("1.txt", file.getFilePath().getFileName(), "Имя файла");
        Assertions.assertEquals("txt", file.getFilePath().getExtension(), "Расширение");
        Assertions.assertEquals("/hello/world/1.txt", file.getFilePath().getPath(), "Полный путь");

        file = new File("/1.txt", () -> null);
        Assertions.assertEquals("/1.txt", file.getFilePath().getPath(), "Полный путь");

        file = new File("1.txt", () -> null);
        Assertions.assertEquals("1.txt", file.getFilePath().getPath(), "Полный путь");

        file = new File("test/1.txt", () -> null);
        Assertions.assertEquals("test/1.txt", file.getFilePath().getPath(), "Полный путь");
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
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        securityComponent.getProperty().setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        securityComponent.loadKeyStorage(password);
        securityComponent.add("test", "12345".toCharArray(), password);

        File file = new File("/test.p12", FileLoaderFactory.createKeyStore("one.jks", "test"));
        file.setSaver(FileSaverFactory.writeFile("one.jks"));
        file.setRepositoryMap("securityKey", "test");
        UtilFile.remove("one.jks");
        UtilFile.removeIfExist("unit-test.jks");
    }

    @Test
    void testComponent() {
        Manager.Configuration<File> fileConfiguration = App.get(Manager.class).configure(
                File.class,
                "hello/world/1.txt",
                path -> new File(path, FileLoaderFactory.fromString("Hello world", "UTF-8"))
        );

        File file1 = fileConfiguration.get();

        Assertions.assertEquals("1.txt", file1.getFilePath().getFileName());
    }

    @Test
    void getExtension() {
        Assertions.assertEquals("txt", UtilUri.parsePath("/test/1.txt").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("//test//1.txt").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("\test\1.txt").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("\\test\\1.txt").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("http://test.com/1.txt").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("http://test.com/1.txt?x=y").getExtension());
        Assertions.assertEquals("txt", UtilUri.parsePath("http://test.com/1.txt?x=y/e.pdf").getExtension());
    }

    @Test
    void testFilePath() {
        Assertions.assertEquals("""
              {"path":"3/2/1.txt","folder":"3/2","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("3/2/1.txt"), "{}")
        );
        Assertions.assertEquals("""
               {"path":"2/1.txt","folder":"2","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("2/1.txt"), "{}")
        );
        Assertions.assertEquals("""
               {"path":"1.txt","folder":null,"fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("2/../1.txt"), "{}")
        );
        Assertions.assertThrowsExactly(RuntimeException.class,
                () -> UtilJson.toString(UtilUri.parsePath("../1.txt"), "{}")
        );
        Assertions.assertEquals("""
               {"path":"/1.txt","folder":"","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/1/2/../../1.txt"), "{}")
        );
        Assertions.assertEquals("""
               {"path":"/1/1.txt","folder":"/1","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/1/2/../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"/1.txt","folder":"","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/test/../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"http://test.com/1.txt?x=y/e.pdf","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":"x=y/e.pdf"}""",
                UtilJson.toString(UtilUri.parsePath("http://test.com/1.txt?x=y/e.pdf"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"http://test.com/1.txt?","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":""}""",
                UtilJson.toString(UtilUri.parsePath("http://test.com/1.txt?"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"http://test.com/1.txt","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("http://test.com/1.txt"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"/test/1","folder":"/test","fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/test/1"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"/test/1.","folder":"/test","fileName":"1.","extension":null,"parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/test/1."), "{}")
        );
        Assertions.assertEquals("""
                {"path":"/1","folder":"","fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("/1"), "{}")
        );
        Assertions.assertEquals("""
                {"path":"1","folder":null,"fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(UtilUri.parsePath("1"), "{}")
        );

    }

}