AmetystKQ
==========

![AmetystKQ Logo](logo.png)

A low-resource optimized Paper 1.21.11 fork for 20-player servers.

**Links:**
- FAQ: [kq.ametystmc.net/faq](https://kq.ametystmc.net/faq)
- Discord: [kq.ametystmc.net/discord](https://kq.ametystmc.net/discord)
- Website: [kq.ametystmc.net](https://kq.ametystmc.net)

## Specifications
| Metric | Value |
|---|---|
## Specifications
| Metric | Value |
|---|---|
| **Memory** | 1GB minimum, 200 players on 16GB |
| **View Distance** | 5 chunks |
| **Entity Tracking** | Players: 48, Animals: 24, Monsters: 32, Misc: 16 |
| **Broadcast Interval** | 6 ticks |
| **Chunk Send Rate** | 15/s |
| **Chunk Load Rate** | 25/s |
| **Chunk Gen Rate** | 5/s |
| **Item Despawn** | 3000 ticks (2.5 min) |
| **Hopper Transfer** | 20 ticks |
| **I/O Threads** | CPU/4 |
| **Chunk Cache** | 128MB–1024MB |
| **Java** | 21+ (ZGC recommended) |
| **Compression** | Velocity libdeflate + OpenSSL |
| **JAR Size** | ~53 MB |

## Benchmarks (Java 26 ZGC, idle, no players)
| RAM | Startup | Idle Memory | TPS (60s) |
|-----|---------|-------------|-----------|
| **2GB** | 19.1s | 1973MB | 20.0 |
| **4GB** | 20.1s | 2878MB | 20.0 |
| **8GB** | 23.1s | 2714MB | 20.0 |
| **View Distance** | 5 chunks |
| **Entity Tracking** | Players: 48, Animals: 24, Monsters: 32, Misc: 16 |
| **Broadcast Interval** | 6 ticks |
| **Chunk Send Rate** | 15/s |
| **Chunk Load Rate** | 25/s |
| **Chunk Gen Rate** | 5/s |
| **Item Despawn** | 3000 ticks (2.5 min) |
| **Hopper Transfer** | 20 ticks |
| **I/O Threads** | CPU/4 |
| **Chunk Cache** | 128MB–1024MB |
| **Java** | 21+ (ZGC recommended) |
| **Compression** | Velocity libdeflate + OpenSSL |
| **JAR Size** | ~53 MB |

How To (Server Admins)
------
AmetystKQ is distributed as a paperclip jar. Download from [kq.ametystmc.net](https://kq.ametystmc.net) or from the [GitHub Releases](https://github.com/Az7car/ametystkq/releases).

```
java -Xms1G -Xmx1G -XX:+UseZGC -jar AmetystKQ-1.21.11.jar --nogui
```

How To (Compiling From Source)
------
Requires JDK 21 and an internet connection.

```bash
./gradlew applyPatches
./gradlew createMojmapPaperclipJar
```

The compiled jar will be in `paper-server/build/libs/`.

Support
------
- FAQ: [kq.ametystmc.net/faq](https://kq.ametystmc.net/faq)
- Discord: [kq.ametystmc.net/discord](https://kq.ametystmc.net/discord)
