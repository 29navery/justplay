package dev.navery.justplay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BundledProtocolTest {
    @TempDir Path directory;

    @Test void bundledEngineReallyUsesJava263PacketCodec() throws Exception {
        Path engine = directory.resolve("geyser.jar");
        try (var input = getClass().getResourceAsStream("/bridge/Geyser-Standalone.jar")) {
            assertNotNull(input, "The mod must contain the translation engine");
            Files.copy(input, engine);
        }
        // Load the engine's actual packet registry, independently of Minecraft
        // and our own metadata. This catches bundling a 26.2 engine by mistake.
        try (var loader = new URLClassLoader(new java.net.URL[]{engine.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> minecraftCodec = loader.loadClass("org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec");
            Object codec = minecraftCodec.getField("CODEC").get(null);
            Object version = codec.getClass().getMethod("getMinecraftVersion").invoke(codec);
            assertEquals("26.3", version);
            int protocol = (int) codec.getClass().getMethod("getProtocolVersion").invoke(codec);
            assertTrue(protocol > 0, "A real packet codec must have a valid protocol ID");
        }
    }
}
