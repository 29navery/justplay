package dev.navery.justplay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class JustPlayMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("justplay");
    private final BridgeProcess bridge = new BridgeProcess(FabricLoader.getInstance().getConfigDir().resolve("justplay"));

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> bridge.stop());
        // Handles JVM shutdown too; normal Minecraft shutdown remains the primary path.
        Runtime.getRuntime().addShutdownHook(new Thread(bridge::stop, "justplay-shutdown"));
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(Commands.literal("justplay")
                        .requires(source -> source.getEntity() == null)
                        .then(Commands.literal("status").executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal(bridge.status()), false);
                            return bridge.isRunning() ? 1 : 0;
                        }))
                        .then(Commands.literal("start").executes(context -> {
                            start(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal(bridge.status()), false);
                            return bridge.isRunning() ? 1 : 0;
                        }))
                        .then(Commands.literal("stop").executes(context -> {
                            bridge.stop();
                            context.getSource().sendSuccess(() -> Component.literal(bridge.status()), false);
                            return 1;
                        }))));
    }

    private void start(MinecraftServer server) {
        try {
            String host = server.getLocalIp();
            if (host == null || host.isBlank() || host.equals("0.0.0.0") || host.equals("::")) host = "127.0.0.1";
            if (bridge.start(host, server.getPort())) {
                LOGGER.info("JustPlay launched Geyser. Open UDP 19132; see config/justplay/bridge.log for readiness and compatibility errors.");
            }
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("JustPlay could not launch the Bedrock bridge. Java server remains available.", e);
        }
    }
}
