AmetystKQ
==========

![AmetystKQ Logo](source/logo.png)

A performance-optimized Paper 1.21.11 fork for up to 200 players.

**Links:**
- FAQ: [kq.ametystmc.net/faq](https://kq.ametystmc.net/faq)
- Discord: [kq.ametystmc.net/discord](https://kq.ametystmc.net/discord)
- Website: [kq.ametystmc.net](https://kq.ametystmc.net)

## Directory Layout

```
AmethystKQ/
├── source/              # Source code & build system
│   ├── paper-server/    # Server implementation
│   ├── paper-api/       # Bukkit/Paper API
│   ├── test-plugin/     # Test plugin
│   ├── build.gradle.kts # Gradle build
│   └── gradlew          # Gradle wrapper
├── server/              # Server runtime files
│   ├── config/          # Configuration (paper-global.yml, ametystkq-config.yml)
│   ├── plugins/         # Bukkit plugins
│   ├── world/           # World data (generated)
│   ├── logs/            # Server logs
│   ├── eula.txt         # EULA agreement
│   └── server.properties
├── scripts/             # Utility scripts
│   ├── start.sh         # Auto-tuning startup script
│   └── apatch.sh        # Patch helper
├── benchmark/           # Benchmark results
├── build/               # Gradle build output
└── libraries/           # Game libraries & assets
```

## Specifications

| Metric | Value |
|---|---|
| **Memory** | 1GB minimum, 200 players on 16GB |
| **View Distance** | 12 chunks |
| **Entity Tracking** | Players: 48, Animals: 24, Monsters: 32, Misc: 16 |
| **Broadcast Interval** | 4 ticks |
| **Chunk Send Rate** | 25/s |
| **Chunk Load Rate** | 50/s |
| **Chunk Gen Rate** | 10/s |
| **Item Merge Radius** | 1.5 blocks |
| **Item Despawn** | 3000 ticks (2.5 min) |
| **Hopper Transfer** | 20 ticks |
| **Container Update** | 3 ticks |
| **Mob Spawner Tick** | 4 ticks |
| **Hanging Tick Freq** | 400 ticks |
| **Compression** | Level 256 (max) |
| **Auto-Save** | 12000 ticks (10 min) |
| **I/O Threads** | CPU/4 |
| **Chunk Cache** | 64MB–1024MB (auto) |
| **Config File** | `server/config/ametystkq-config.yml` |
| **Java** | 21+ (ZGC recommended) |
| **JAR Size** | ~53 MB |

## Configuration

AmetystKQ generates `server/config/ametystkq-config.yml` on first run:

```yaml
disable-phantom-spawning: false    # Disable phantom mob spawning
disable-patrol-spawning: false     # Disable pillager patrol spawning
disable-wandering-trader: false    # Disable wandering traders
disable-village-siege: false       # Disable zombie sieges
disable-raid: false                # Disable raids entirely
disable-trial-spawner: false       # Disable trial spawners
item-merge-radius: 1               # Item merge distance (blocks)
item-despawn-ticks: 3000           # Item lifetime (6000 = 5min)
arrow-despawn-ticks: 300           # Arrow lifetime
entity-activation-range:           # AI activation distance
  monsters: 24
  animals: 12
mob-cap:                           # Per-player mob limits
  monster: 40
  animal: 8
memory-profile: auto               # RAM tuning: low/medium/high
```

The server uses online mode by default.

## Quick Start

```bash
# Build
cd source && ./gradlew applyPatches && ./gradlew createMojmapPaperclipJar

# Run (auto-detects hardware)
cd .. && ./scripts/start.sh
```

## Benchmarks (Java 26 ZGC, AMD Zen 4, 8 cores)

Full report: `benchmark/BENCHMARK.md`

| Profile | Startup | Idle TPS | 300 Entity TPS | Recovery TPS |
|---------|---------|----------|----------------|--------------|
| **1GB** | 19.6s   | 20.0     | 19.7           | 20.0         |
| **2GB** | 18.7s   | 20.0     | 19.7           | 20.0         |
| **4GB** | 19.0s   | 20.0     | 19.7           | 20.0         |

- **40% faster startup** than upstream Paper (CDS + async DFU)
- **200+ entities** at 20.0 TPS on 1GB RAM
- **ZGC** eliminates GC lag spikes
- **World on disk**: ~2.4MB after spawn generation

## Building From Source

Requires JDK 21+ and an internet connection.

```bash
cd source
./gradlew applyPatches
./gradlew createMojmapPaperclipJar
```

The compiled jar will be in `source/paper-server/build/libs/`.

## Support

- FAQ: [kq.ametystmc.net/faq](https://kq.ametystmc.net/faq)
- Discord: [kq.ametystmc.net/discord](https://kq.ametystmc.net/discord)
