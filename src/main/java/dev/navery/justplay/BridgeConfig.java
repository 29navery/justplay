package dev.navery.justplay;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Lifecycle settings; protocol and authentication settings live in Geyser's config.yml. */
public record BridgeConfig(boolean enabled, int maxHeapMb, Path jar) {
    public static BridgeConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("justplay.properties");
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) { properties.load(reader); }
        } else {
            properties.setProperty("enabled", "true");
            properties.setProperty("max-heap-mb", "512");
            properties.setProperty("geyser-jar", "bundled");
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "JustPlay: use bundled or a jar path relative to this directory");
            }
        }
        String enabled = properties.getProperty("enabled", "true");
        if (!enabled.equals("true") && !enabled.equals("false")) {
            throw new IOException("enabled must be true or false");
        }
        int heap;
        try { heap = Integer.parseInt(properties.getProperty("max-heap-mb", "512")); }
        catch (NumberFormatException e) { throw new IOException("max-heap-mb must be a number", e); }
        if (heap < 128 || heap > 32768) throw new IOException("max-heap-mb must be 128..32768");
        String jar = properties.getProperty("geyser-jar", "bundled");
        return new BridgeConfig(Boolean.parseBoolean(enabled), heap,
                jar.equals("bundled") ? null : directory.resolve(jar).toAbsolutePath());
    }

    public static String initialGeyserConfig(String host, int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid Java server port");
        // Quote YAML scalars, including IPv6 and unusual bind addresses.
        String quotedHost = "'" + host.replace("'", "''") + "'";
        return """
                # Generated once by JustPlay. Geyser expands this with its other defaults.
                # Keep online-mode=true on the Java server.
                config-version: 5
                bedrock:
                  address: 0.0.0.0
                  port: 19132
                java:
                  address: %s
                  port: %d
                  auth-type: online
                """.formatted(quotedHost, port);
    }
}
