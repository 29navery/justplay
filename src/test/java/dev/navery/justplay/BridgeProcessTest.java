package dev.navery.justplay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class BridgeProcessTest {
    @TempDir Path directory;

    @Test void configDefaultsAndValidation() throws Exception {
        BridgeConfig defaults = BridgeConfig.load(directory);
        assertTrue(defaults.enabled());
        assertEquals(512, defaults.maxHeapMb());
        assertNull(defaults.jar());
        Files.writeString(directory.resolve("justplay.properties"), "max-heap-mb=oops\n");
        assertThrows(java.io.IOException.class, () -> BridgeConfig.load(directory));
    }

    @Test void disabledBridgeDoesNotLaunch() throws Exception {
        Files.writeString(directory.resolve("justplay.properties"), "enabled=false\n");
        BridgeProcess bridge = new BridgeProcess(directory);
        assertFalse(bridge.start("127.0.0.1", 25565));
        assertFalse(bridge.isRunning());
        assertFalse(Files.exists(directory.resolve("config.yml")));
    }

    @Test void processStopsRestartsAndPreservesConfiguration() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, FakeGeyser.class.getName());
        String resource = FakeGeyser.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(directory.resolve("fake.jar")), manifest);
             var input = FakeGeyser.class.getClassLoader().getResourceAsStream(resource)) {
            jar.putNextEntry(new JarEntry(resource));
            input.transferTo(jar);
            jar.closeEntry();
        }
        Files.writeString(directory.resolve("justplay.properties"), "geyser-jar=fake.jar\n");
        Files.writeString(directory.resolve("config.yml"), "# user settings\n");
        BridgeProcess bridge = new BridgeProcess(directory);
        try {
            assertTrue(bridge.start("127.0.0.1", 25565));
            assertFalse(bridge.start("127.0.0.1", 25565));
            assertTrue(bridge.isRunning());
            assertEquals("# user settings\n", Files.readString(directory.resolve("config.yml")));
        } finally { bridge.stop(); }
        assertFalse(bridge.isRunning());
        try { assertTrue(bridge.start("127.0.0.1", 25565)); }
        finally { bridge.stop(); }
        assertFalse(bridge.isRunning());
    }

    @Test void targetPortAndAddressAreValidYaml() {
        String yaml = BridgeConfig.initialGeyserConfig("::1", 25570);
        assertTrue(yaml.contains("address: '::1'"));
        assertTrue(yaml.contains("port: 25570"));
        assertTrue(yaml.contains("auth-type: online"));
        assertThrows(IllegalArgumentException.class, () -> BridgeConfig.initialGeyserConfig("localhost", -1));
    }

    public static class FakeGeyser {
        public static void main(String[] args) throws Exception {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null || line.equals("geyser stop")) return;
                }
            }
        }
    }
}
