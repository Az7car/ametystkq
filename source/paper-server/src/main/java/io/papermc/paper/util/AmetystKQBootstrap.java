package io.papermc.paper.util;

import io.papermc.paper.configuration.AmetystKQConfig;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.configuration.WorldConfiguration;
import io.papermc.paper.util.nbt.NbtOptimizer;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AmetystKQBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger("AmetystKQ");
    private static volatile boolean initialized;
    private static int ioThreads;
    private static int cacheSizeMb;
    private static int prefetchRadius;

    public static int MAX_VIEW_DISTANCE = 12;
    public static int BROADCAST_INTERVAL = 4;
    public static AmetystKQConfig CONFIG;

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
        final int cpuCores = Runtime.getRuntime().availableProcessors();

        final boolean lowMem = maxMemoryMb <= 2048;
        final boolean veryLowMem = maxMemoryMb <= 1024;

        ioThreads = Math.max(1, cpuCores <= 2 ? 1 : cpuCores / 4);
        cacheSizeMb = veryLowMem ? 64 : Math.max(64, Math.min(maxMemoryMb / 8, lowMem ? 512 : 1024));
        prefetchRadius = lowMem ? 1 : 2;

        LOGGER.info("AmetystKQ performance systems initialized");
        LOGGER.info("  Max memory: {}MB, CPU cores: {}, I/O threads: {}, Cache: {}MB", maxMemoryMb, cpuCores, ioThreads, cacheSizeMb);

        io.papermc.paper.entity.tracker.EntityBroadcastController.BROADCAST_INTERVAL = BROADCAST_INTERVAL;

        LOGGER.info("  Bandwidth: viewDist={}, broadcast={}t, {} cores",
            MAX_VIEW_DISTANCE, BROADCAST_INTERVAL, cpuCores);

        applySystemOverrides(lowMem);

        CONFIG = AmetystKQConfig.load(Path.of("config"));
        applyConfig(server, cpuCores);

        initNbtOptimizer();
        initDataFixerBypass();

        Runtime.getRuntime().addShutdownHook(new Thread(AmetystKQBootstrap::shutdown, "AmetystKQ Shutdown"));
    }

    private static void applyConfig(final MinecraftServer server, final int cpuCores) {
        if (CONFIG == null) return;

        LOGGER.info("  Overrides: phantom={} patrol={} trader={} siege={} cat={} raid={} trial={} bee={} dolphin={} ocelot={} frog={} pillager={}",
            CONFIG.disablePhantomSpawning, CONFIG.disablePatrolSpawning, CONFIG.disableWanderingTrader,
            CONFIG.disableVillageSiege, CONFIG.disableCatSpawner, CONFIG.disableRaid,
            CONFIG.disableTrialSpawner, CONFIG.disableBeeSpawner, CONFIG.disableDolphinSpawner,
            CONFIG.disableOcelotSpawner, CONFIG.disableFrogSpawner, CONFIG.disablePillagerPatrols);
        LOGGER.info("  Rates: autoSave={}t hopper={}t container={}t spawner={}t hanging={}t fire={}t",
            CONFIG.autoSaveTicks, CONFIG.hopperTransferTicks, CONFIG.containerUpdateTicks,
            CONFIG.mobSpawnerTickRate, CONFIG.hangingTickFreq, CONFIG.fireTickRate);
        LOGGER.info("  Items: merge={} xpMerge={} despawn={} arrow={}",
            CONFIG.itemMergeRadius, CONFIG.xpMergeRadius, CONFIG.itemDespawnTicks, CONFIG.arrowDespawnTicks);
        LOGGER.info("  Activation: monster={} animal={} water={} misc={} collisions={} sim={}",
            CONFIG.entityActivationRangeMonsters, CONFIG.entityActivationRangeAnimals,
            CONFIG.entityActivationRangeWater, CONFIG.entityActivationRangeMisc,
            CONFIG.maxEntityCollisions, CONFIG.simulationDistance);

        final GlobalConfiguration global = GlobalConfiguration.get();
        global.playerAutoSave.rate = CONFIG.autoSaveTicks;
        global.chunkLoadingBasic.playerMaxChunkSendRate = Math.max(5, Math.min(50, 50 - CONFIG.entityActivationRangeMonsters / 8));
        global.chunkLoadingBasic.playerMaxChunkLoadRate = Math.max(10, Math.min(80, cpuCores * 8));
        global.chunkLoadingBasic.playerMaxChunkGenerateRate = Math.max(2, Math.min(20, cpuCores));
        global.collisions.enablePlayerCollisions = CONFIG.maxEntityCollisions > 0;

        int worldCount = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            worldCount++;

            final var spigot = level.spigotConfig;

            spigot.hopperTransfer = CONFIG.hopperTransferTicks;
            spigot.itemMerge = CONFIG.itemMergeRadius;
            spigot.expMerge = CONFIG.xpMergeRadius;
            spigot.itemDespawnRate = CONFIG.itemDespawnTicks;
            spigot.arrowDespawnRate = CONFIG.arrowDespawnTicks;
            spigot.hangingTickFrequency = CONFIG.hangingTickFreq;
            spigot.monsterActivationRange = CONFIG.entityActivationRangeMonsters;
            spigot.animalActivationRange = CONFIG.entityActivationRangeAnimals;
            spigot.waterActivationRange = CONFIG.entityActivationRangeWater;
            spigot.miscActivationRange = CONFIG.entityActivationRangeMisc;

            if (CONFIG.simulationDistance > 0) {
                spigot.simulationDistance = CONFIG.simulationDistance;
            }

            final WorldConfiguration wc = level.paperConfig();

            final var spawning = wc.entities.spawning;
            final var behavior = wc.entities.behavior;
            final var env = wc.environment;
            final var ticks = wc.tickRates;
            final var coll = wc.collisions;
            final var hopper = wc.hopper;
            final var misc = wc.misc;

            if (CONFIG.disablePhantomSpawning) {
                behavior.phantomsSpawnAttemptMaxSeconds = 0;
            }
            if (CONFIG.disablePillagerPatrols) {
                behavior.pillagerPatrols.disable = true;
            }
            if (CONFIG.disableWanderingTrader) {
                spawning.wanderingTrader.spawnDayLength = Integer.MAX_VALUE;
            }
            if (CONFIG.disableThunder) {
                env.disableThunder = true;
            }
            if (CONFIG.disableIceAndSnow) {
                env.disableIceAndSnow = true;
            }
            if (CONFIG.enableAlternateCurrent) {
                misc.redstoneImplementation = WorldConfiguration.Misc.RedstoneImplementation.ALTERNATE_CURRENT;
            }

            if (CONFIG.mobCapMonster >= 0) spawning.spawnLimits.put(MobCategory.MONSTER, CONFIG.mobCapMonster);
            if (CONFIG.mobCapAnimal >= 0) spawning.spawnLimits.put(MobCategory.CREATURE, CONFIG.mobCapAnimal);
            if (CONFIG.mobCapWater >= 0) spawning.spawnLimits.put(MobCategory.WATER_CREATURE, CONFIG.mobCapWater);
            if (CONFIG.mobCapWaterAmbient >= 0) spawning.spawnLimits.put(MobCategory.WATER_AMBIENT, CONFIG.mobCapWaterAmbient);
            if (CONFIG.mobCapUndergroundWater >= 0) spawning.spawnLimits.put(MobCategory.UNDERGROUND_WATER_CREATURE, CONFIG.mobCapUndergroundWater);

            env.fireTickDelay = CONFIG.fireTickRate;
            ticks.mobSpawner = CONFIG.mobSpawnerTickRate;
            ticks.containerUpdate = CONFIG.containerUpdateTicks;
            coll.maxEntityCollisions = CONFIG.maxEntityCollisions;

            hopper.cooldownWhenFull = true;
            hopper.disableMoveEvent = true;

            if (CONFIG.disableFluidUpdates) {
                env.maxFluidTicks = 0;
            }
        }

        LOGGER.info("AmetystKQ config applied across {} worlds", worldCount);
    }

    private static void applySystemOverrides(final boolean lowMem) {
        System.setProperty("Paper.maxTickTime", lowMem ? "200" : "100");
        System.setProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", "false");
        System.setProperty("Paper.ignoreWorldDataVersion", "true");

        if (lowMem) {
            System.setProperty("minecraft.chunk.io.maxConcurrent", "2");
        } else {
            System.setProperty("minecraft.chunk.io.maxConcurrent", "4");
        }

        if (Runtime.getRuntime().maxMemory() <= 1024 * 1024 * 1024) {
            System.setProperty("io.netty.allocator.tinyCacheSize", "4096");
            System.setProperty("io.netty.allocator.smallCacheSize", "4096");
            System.setProperty("io.netty.allocator.normalCacheSize", "4096");
        }

        System.setProperty("DataFixerUpper.async", "true");
        System.setProperty("Paper.skipServerPropertiesValidation", "true");
        System.setProperty("sun.management.jmxremote", "false");
        System.setProperty("minecraft.landCacheSize", "256");
        System.setProperty("Paper.skipInvalidRecipeFiles", "true");
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
