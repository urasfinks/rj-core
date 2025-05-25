package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.ManagerConfigurationFactory;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilUri;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStore;
import ru.jamsys.core.resource.virtual.file.system.ReadFromSourceFactory;
import ru.jamsys.core.resource.virtual.file.system.WriteToDestinationFactory;

import java.nio.charset.StandardCharsets;

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
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/1.txt",
                file -> file.setupReadFromSource(() -> null)
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("/hello/world", file.getFilePath().getFolder(), "Директория");
        Assertions.assertEquals("1.txt", file.getFilePath().getFileName(), "Имя файла");
        Assertions.assertEquals("txt", file.getFilePath().getExtension(), "Расширение");
        Assertions.assertEquals("/hello/world/1.txt", file.getFilePath().getPath(), "Полный путь");
    }

    @Test
    public void testMain2() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/1.txt",
                file -> file.setupReadFromSource(() -> null)
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("/1.txt", file.getFilePath().getPath(), "Полный путь");
    }

    @Test
    public void testMain3() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "1.txt",
                file -> file.setupReadFromSource(() -> null)
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("1.txt", file.getFilePath().getPath(), "Полный путь");
    }

    @Test
    public void testMain4() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "test/1.txt",
                file -> file.setupReadFromSource(() -> null)
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("test/1.txt", file.getFilePath().getPath(), "Полный путь");
    }

    @Test
    public void testGetString() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/1_1.txt",
                file1 -> file1.setupReadFromSource(ReadFromSourceFactory.fromString("Hello world", "UTF-8"))
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("Hello world", new String(file.getBytes(), StandardCharsets.UTF_8), "#1");
    }

    @Test
    public void testGetString2() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/2.txt",
                file1 -> file1.setupReadFromSource(ReadFromSourceFactory.fromBase64("SGVsbG8gd29ybGQ=", "UTF-8"))
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("Hello world", new String(file.getBytes(), StandardCharsets.UTF_8), "#1");
    }

    @Test
    public void testGetBase64() throws Exception {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/3.txt",
                file1 -> file1.setupReadFromSource(ReadFromSourceFactory.fromString("Hello world", "UTF-8"))
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertEquals("SGVsbG8gd29ybGQ=", UtilBase64.encode(file.getBytes(), false), "#1");
    }

    @Test
    public void testFromFileSystem() throws Exception {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/4.txt",
                file1 -> file1.setupReadFromSource(ReadFromSourceFactory.fromFileSystem("pom.xml"))
        );
        File file = fileManagerConfiguration.get();
        Assertions.assertFalse(new String(file.getBytes(), StandardCharsets.UTF_8).isEmpty(), "#1");
    }

    @Test
    public void testKeyStore() throws Exception {
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        securityComponent.getProperty().setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        securityComponent.loadKeyStorage(password);
        securityComponent.add("test", "12345".toCharArray(), password);

        ManagerConfiguration<FileKeyStore> fileManagerConfiguration = ManagerConfigurationFactory.get(
                FileKeyStore.class,
                "/hello/world/5.txt",
                file1 -> {
                    file1.setupReadFromSource(ReadFromSourceFactory.createKeyStoreAndRead("one.jks", "test"));
                    file1.setupWriteToDestination(WriteToDestinationFactory.writeFile("one.jks"));
                    file1.setupSecurityAlias("test");
                }
        );
        File file = fileManagerConfiguration.get();
        UtilFile.remove("one.jks");
        UtilFile.removeIfExist("unit-test.jks");
    }

    @Test
    void testComponent() {
        ManagerConfiguration<File> fileManagerConfiguration = ManagerConfigurationFactory.get(
                File.class,
                "/hello/world/6.txt",
                file1 -> file1.setupReadFromSource(ReadFromSourceFactory.fromString("Hello world", "UTF-8"))
        );
        File file1 = fileManagerConfiguration.get();
        Assertions.assertEquals("6.txt", file1.getFilePath().getFileName());
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