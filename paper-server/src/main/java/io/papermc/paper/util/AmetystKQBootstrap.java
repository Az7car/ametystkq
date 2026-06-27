package io.papermc.paper.util;

import io.papermc.paper.util.nbt.NbtOptimizer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AmetystKQBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger("AmetystKQ");
    private static volatile boolean initialized;
    private static int ioThreads;
    private static int cacheSizeMb;
    private static int prefetchRadius;

    public static int MAX_VIEW_DISTANCE = 5;
    public static int BROADCAST_INTERVAL = 6;

    private static final String WATERMARK =
        "\n __   ____  ____  ___   __   ____ " +
        "\n / _\\ (__  )(__  )/ __) / _\\ (  _ \\" +
        "\n/    \\ / _/   / /( (__ /    \\ )   /" +
        "\n\\_/\\_/(____) (_/  \\___)\\_/\\_/(__\\_)\n";

    private AmetystKQBootstrap() {}

    public static void init(final MinecraftServer server) {
        if (initialized) return;
        initialized = true;

        System.out.println(WATERMARK);

        final long maxMemory = Runtime.getRuntime().maxMemory();
        final int maxMemoryMb = (int) (maxMemory / (1024 * 1024));
        ioThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 4);
        cacheSizeMb = Math.max(128, Math.min(maxMemoryMb / 8, 1024));
        prefetchRadius = 2;

        LOGGER.info("AmetystKQ performance systems initialized");
        LOGGER.info("  Max memory: {}MB, I/O threads: {}, Cache: {}MB", maxMemoryMb, ioThreads, cacheSizeMb);
        LOGGER.info("  Bandwidth: maxViewDistance={}, broadcastInterval={}ticks, chunkSendRate={}/s", MAX_VIEW_DISTANCE, BROADCAST_INTERVAL, 15);

        io.papermc.paper.entity.tracker.EntityBroadcastController.BROADCAST_INTERVAL = BROADCAST_INTERVAL;

        initNbtOptimizer();
        initDataFixerBypass();

        Runtime.getRuntime().addShutdownHook(new Thread(AmetystKQBootstrap::shutdown, "AmetystKQ Shutdown"));
    }

    private static void initNbtOptimizer() {
        NbtOptimizer.setEnabled(true);
    }

    private static void initDataFixerBypass() {
        DataFixerBypass.setGloballyDisabled(true);
        System.setProperty("Paper.minPrecachedDatafixVersion", "9999");
    }

    public static int ioThreads() { return ioThreads; }
    public static int cacheSizeMb() { return cacheSizeMb; }
    public static int prefetchRadius() { return prefetchRadius; }

    public static void shutdown() {
        if (!initialized) return;
        LOGGER.info("AmetystKQ shutdown complete");
        initialized = false;
    }

    public static boolean isInitialized() { return initialized; }
}
