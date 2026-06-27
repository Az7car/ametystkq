Contributing to AmetystKQ
===========================

AmetystKQ is a fork of Paper 1.21.11 focused on performance for up to 200 players on cheap hardware.

## Building

Requires JDK 21+ and an internet connection.

```bash
cd source
./gradlew applyPatches
./gradlew createMojmapPaperclipJar
```

The compiled jar is at `source/paper-server/build/libs/`.

## Project Structure

- `source/paper-server/` — Server implementation (patches apply to Minecraft sources in `src/minecraft/`)
- `source/paper-api/` — Bukkit/Paper API
- `server/` — Server runtime (worlds, config, logs)
- `scripts/` — Build & run scripts
- `benchmark/` — Performance benchmark results

## Making Changes

1. Edit source files in `paper-server/src/main/java/` (AmetystKQ code) or `paper-server/patches/` (Minecraft patches).
2. Rebuild with `./gradlew createMojmapPaperclipJar`.
3. Test with `../scripts/start.sh`.

## Support

- FAQ: https://kq.ametystmc.net/faq
- Discord: https://kq.ametystmc.net/discord
