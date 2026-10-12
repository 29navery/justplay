# JustPlay

A server-side Fabric mod for Minecraft Java **26.3** that manages a bundled
[Geyser](https://geysermc.org/) bridge. Geyser translates Bedrock packets into
Java packets and Java world, inventory, entity and interaction updates back into
Bedrock packets. JustPlay controls configuration and process lifecycle; it is
not an independent implementation of the two protocols.

## Compatibility status

Bundled engine: official **Geyser Java 26.3 preview, PR #6712 build 3095**,
SHA-256 verified. This engine includes Java 26.3 packet codecs and content
mappings; it does not merely change the advertised version. It connects to the
Java server as a **26.3** client, so ViaBackwards is not required solely to bridge
Geyser's former 26.2 protocol to this server.

**Experimental:** upstream's [26.3 update](https://github.com/GeyserMC/Geyser/pull/6712)
is incomplete and warns against production use. Bedrock gameplay still needs
live testing. Use a **26.3-compatible Floodgate build** if using Floodgate;
upstream explicitly notes that Floodgate also needs updating.

### Upgrade from JustPlay 1.0.0

Stop the Java server normally and make sure its previous Geyser child has exited.
Replace the old JustPlay jar in `mods/` with `justplay-1.1.0-beta.1+mc26.3.jar`.
In `config/justplay/justplay.properties`, use `geyser-jar=bundled` to select the
new engine (an explicit custom jar path continues to override the bundle).
Existing Geyser/Floodgate settings are preserved. Restart once; the engine is
extracted under a new version-specific filename, so the old jar isn't reused.
Do not start extracted Geyser jars manually alongside JustPlay.

## Install

1. Run a dedicated Minecraft 26.3 server with Java 25, Fabric Loader 0.19.5+
   and Fabric API for 26.3.
2. Put `justplay-1.1.0-beta.1+mc26.3.jar` in `mods/`. Geyser is bundled; do not install
   another Geyser listener on the same port.
3. Start the server. JustPlay creates `config/justplay/`, extracts its engine,
   generates initial configuration and starts a separate Java process. Your
   host must allow child processes and have memory for both Minecraft and Geyser.
4. Check `config/justplay/bridge.log` for listener startup and compatibility
   errors. Open/forward **UDP 19132** in your firewall/router/hosting panel.
5. Bedrock players add your public address with port **19132**. Console editions
   may require a separate method to add custom servers.

JustPlay does not provide hosting, tunneling or automatic port forwarding.
It launches after the Java server starts and stops with the server. Launch
failures leave the Java server available. Java client-required mods and arbitrary
custom content do not become Bedrock-compatible simply by installing a bridge.

## Authentication

The default `java.auth-type: online` requires a Microsoft account owning Java
Edition. Keep `online-mode=true` on the Java server.

For Bedrock accounts without Java ownership, install a compatible Fabric
Floodgate mod, set `java.auth-type: floodgate` in the Geyser config, and set
`advanced.floodgate-key-file` to Floodgate's key (prefer an absolute path).
Floodgate is not bundled. See [Floodgate setup](https://geysermc.org/wiki/floodgate/setup/).

## Configuration and console commands

- `config/justplay/justplay.properties`: `enabled=true`, `max-heap-mb=512`,
  `geyser-jar=bundled`. To use an updated engine, supply an absolute jar path
  or one relative to `config/justplay/`. It must support Geyser's `--nogui
  --config` arguments and a compatible native YAML schema.
- `config/justplay/config.yml`: native Geyser settings, generated once using
  the Java server's actual bind address and port. Existing settings are preserved.
  Update the target manually if the Java port/address changes afterward.
- `config/justplay/bridge.log`: Geyser stdout/stderr. Other Geyser files,
  resource packs, extensions and login data live under `config/justplay/`.
- `justplay status`, `justplay stop`, `justplay start`: server console/RCON
  and non-player command sources only. Stop/start to apply configuration changes.
  Status reports process liveness, not client connectivity; check the log.

Unexpected bridge exits are visible in status; there is no automatic restart loop.

## Build

Run `bash gradlew build` with Java 25 (Windows: `gradlew.bat build`). The build
fetches the pinned official engine, verifies SHA-256, and bundles it. Output:
`build/libs/`. GitHub Actions also builds, tests and uploads workflow artifacts.
Tests cover config validation, first-run targets, disabled mode, process
start/stop/restart, preservation of user settings, and the bundled Java packet codec.

## License

JustPlay's original code uses the repository's CC0-1.0 license. The unmodified
Geyser engine uses MIT and retains upstream notices and dependency licenses.
See `bridge/NOTICE.txt` and `bridge/GEYSER-LICENSE.txt` inside the jar. JustPlay
is independent of GeyserMC, Mojang and Microsoft.
