package dev.navery.justplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs Geyser outside Minecraft's class loader, with its own logs and working directory. */
public final class BridgeProcess {
    private static final String SHA256 = "83d4b3acb990be93fbf2ebc968b4037f00154c08dc8e1b1aa324b95b06853de1";
    private final Path directory;
    private Process process;
    private Integer lastExit;

    public BridgeProcess(Path directory) { this.directory = directory.toAbsolutePath(); }

    public synchronized boolean start(String host, int port) throws IOException {
        if (isRunning()) return false;
        BridgeConfig config = BridgeConfig.load(directory);
        if (!config.enabled()) return false;
        Path jar = config.jar() == null ? extractBundledJar() : config.jar();
        if (!Files.isRegularFile(jar)) throw new IOException("Geyser jar does not exist: " + jar);
        Path yaml = directory.resolve("config.yml");
        if (!Files.exists(yaml)) Files.writeString(yaml, BridgeConfig.initialGeyserConfig(host, port));
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        process = new ProcessBuilder(List.of(java, "-Xmx" + config.maxHeapMb() + "M",
                "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true",
                "-jar", jar.toString(), "--nogui", "--config", yaml.toString()))
                .directory(directory.toFile()).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("bridge.log").toFile()))
                .start();
        lastExit = null;
        Process launched = process;
        launched.onExit().thenAccept(exited -> {
            synchronized (this) {
                if (process == exited) lastExit = exited.exitValue();
            }
        });
        return true;
    }

    private Path extractBundledJar() throws IOException {
        Path jar = directory.resolve("Geyser-Standalone-java26.3-pr6712-b3095.jar");
        if (Files.isRegularFile(jar) && checksum(jar).equals(SHA256)) return jar;
        Path temporary = Files.createTempFile(directory, "geyser-", ".tmp");
        try {
            try (var input = BridgeProcess.class.getResourceAsStream("/bridge/Geyser-Standalone.jar")) {
                if (input == null) throw new IOException("Bundled Geyser jar is missing");
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!checksum(temporary).equals(SHA256)) throw new IOException("Bundled Geyser checksum mismatch");
            Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        return jar;
    }

    private static String checksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public synchronized boolean isRunning() { return process != null && process.isAlive(); }

    public synchronized String status() {
        return isRunning() ? "Geyser process running (PID " + process.pid() + "). Check bridge.log for listener readiness."
                : "Geyser process stopped" + (lastExit == null ? "." : " (exit code " + lastExit + ").");
    }

    public synchronized void stop() {
        if (!isRunning()) return;
        // Geyser's shutdown command flushes sessions and closes its UDP listener.
        try {
            process.getOutputStream().write("geyser stop\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            process.getOutputStream().flush();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(3, TimeUnit.SECONDS);
                }
            }
        } catch (IOException e) { process.destroyForcibly(); }
        catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
