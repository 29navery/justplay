# JustPlay

A server-side Fabric mod for Minecraft Java **26.3** that manages a bundled
[Geyser](https://geysermc.org/) bridge. Geyser translates Bedrock packets into
Java packets and Java world, inventory, entity and interaction updates back into
Bedrock packets. JustPlay controls configuration and process lifecycle; it is
not an independent implementation of the two protocols.

## Compatibility status

Bundled engine: official **Geyser Standalone 2.11.3 build 1251**, SHA-256 verified.
Geyser's current documentation lists Java **26.2**, not 26.3. A successful mod
build does not establish that Bedrock clients can join 26.3. Use an engine
supporting 26.3, or a protocol compatibility mod specifically supporting the
Java protocol emitted by your Geyser build. ViaFabric/ViaVersion may provide
this when their version matrix supports it; arbitrary releases are not enough.
Actual Bedrock login and gameplay require testing with compatible live clients.

## Install

1. Run a dedicated Minecraft 26.3 server with Java 25, Fabric Loader 0.19.5+
   and Fabric API for 26.3.
2. Put `justplay-1.0.0+mc26.3.jar` in `mods/`. Geyser is bundled; do not install
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
  --config` arguments and version-5 YAML schema.
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
start/stop/restart and preservation of user settings.

## License

JustPlay's original code uses the repository's CC0-1.0 license. The unmodified
Geyser engine uses MIT and retains upstream notices and dependency licenses.
See `bridge/NOTICE.txt` and `bridge/GEYSER-LICENSE.txt` inside the jar. JustPlay
is independent of GeyserMC, Mojang and Microsoft.
