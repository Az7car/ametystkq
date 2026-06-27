package com.ametystkq.telemetry;

import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;

public final class AmetystKQTelemetry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENDPOINT = "https://ametystkq-telemetry.az7car.workers.dev";
    private static final String ID_FILE = "telemetry-id";
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AmetystKQ-Telemetry");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static String serverId;
    private static boolean started;

    public static void start(MinecraftServer server) {
        if (started) return;
        started = true;

        serverId = loadOrCreateId(server);

        sendPing(server, "start");

        scheduler.scheduleAtFixedRate(() -> sendPing(server, "keepalive"), 5, 5, TimeUnit.MINUTES);
    }

    public static void stop(MinecraftServer server) {
        sendPing(server, "stop");
        scheduler.shutdownNow();
    }

    private static String loadOrCreateId(MinecraftServer server) {
        Path path = server.getServerDirectory().resolve(ID_FILE);
        try {
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8).trim();
            }
            String id = UUID.randomUUID().toString();
            Files.writeString(path, id, StandardCharsets.UTF_8);
            return id;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private static void sendPing(MinecraftServer server, String action) {
        try {
            int playerCount = 0;
            int maxPlayers = 0;
            List<String> playerNames = new ArrayList<>();
            int viewDistance = 8;
            int simDistance = 8;
            if (server.getPlayerList() != null) {
                PlayerList list = server.getPlayerList();
                playerCount = list.getPlayerCount();
                maxPlayers = list.getMaxPlayers();
                viewDistance = list.getViewDistance();
                simDistance = list.getSimulationDistance();
                for (ServerPlayer p : list.getPlayers()) {
                    playerNames.add(p.getScoreboardName());
                }
            }

            double[] tps = server.getTPS();
            double tps1m = tps.length > 0 ? tps[0] : 20.0;
            double tps5m = tps.length > 1 ? tps[1] : 20.0;
            double tps15m = tps.length > 2 ? tps[2] : 20.0;

            int worldCount = 0;
            List<String> worldNames = new ArrayList<>();
            long totalLoadedChunks = 0;
            for (ServerLevel world : server.getAllLevels()) {
                worldCount++;
                worldNames.add(world.dimension().identifier().toString());
                totalLoadedChunks += world.getChunkSource().getLoadedChunksCount();
            }

            Runtime rt = Runtime.getRuntime();
            long maxMemory = rt.maxMemory();
            long totalMemory = rt.totalMemory();
            long freeMemory = rt.freeMemory();
            int availableProcessors = rt.availableProcessors();
            long usedMemory = totalMemory - freeMemory;

            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            String jvmUptime = String.valueOf(runtimeBean.getUptime());
            String playerNamesJson = String.join(",", playerNames).replace("\"", "\\\"");
            String worldNamesJson = String.join(",", worldNames).replace("\"", "\\\"");

            String json = String.format(
                "{\"serverId\":\"%s\",\"action\":\"%s\",\"playerCount\":%d,\"maxPlayers\":%d,\"playerNames\":\"%s\",\"version\":\"%s\",\"serverModName\":\"%s\",\"onlineMode\":\"%s\",\"motd\":\"%s\",\"port\":%d,\"hardcore\":%s,\"viewDistance\":%d,\"simDistance\":%d,\"tps1m\":%.2f,\"tps5m\":%.2f,\"tps15m\":%.2f,\"uptimeMs\":%d,\"jvmUptimeMs\":%s,\"maxMemoryMb\":%d,\"totalMemoryMb\":%d,\"freeMemoryMb\":%d,\"usedMemoryMb\":%d,\"cpus\":%d,\"osName\":\"%s\",\"osArch\":\"%s\",\"javaVersion\":\"%s\",\"worldCount\":%d,\"worldNames\":\"%s\",\"loadedChunks\":%d}",
                serverId,
                action,
                playerCount,
                maxPlayers,
                playerNamesJson,
                server.getServerVersion(),
                server.getServerModName(),
                String.valueOf(server.usesAuthentication()),
                server.getMotd(),
                server.getPort(),
                String.valueOf(server.isHardcore()),
                viewDistance,
                simDistance,
                tps1m,
                tps5m,
                tps15m,
                System.currentTimeMillis() - org.bukkit.craftbukkit.Main.BOOT_TIME.toEpochMilli(),
                jvmUptime,
                maxMemory / 1048576L,
                totalMemory / 1048576L,
                freeMemory / 1048576L,
                usedMemory / 1048576L,
                availableProcessors,
                System.getProperty("os.name", "unknown").replace("\"", "\\\""),
                System.getProperty("os.arch", "unknown").replace("\"", "\\\""),
                System.getProperty("java.version", "unknown").replace("\"", "\\\""),
                worldCount,
                worldNamesJson,
                totalLoadedChunks
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + "/ping"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(BodyPublishers.ofString(json))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(e -> null);
        } catch (Exception e) {
            LOGGER.debug("Failed to send telemetry ping", e);
        }
    }
}
