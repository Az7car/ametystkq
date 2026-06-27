package io.papermc.paper.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class AmetystKQConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("AmetystKQ/Config");
    private static volatile AmetystKQConfig instance;

    public boolean disablePhantomSpawning = false;
    public boolean disablePatrolSpawning = false;
    public boolean disableWanderingTrader = false;
    public boolean disableVillageSiege = false;
    public boolean disableCatSpawner = false;
    public boolean disablePillagerPatrols = false;
    public boolean disableRaid = false;
    public boolean disableTrialSpawner = false;
    public boolean disableBeeSpawner = false;
    public boolean disableDolphinSpawner = false;
    public boolean disableOcelotSpawner = false;
    public boolean disableFrogSpawner = false;

    public int autoSaveTicks = 12000;
    public int mobSpawnerTickRate = 4;
    public int hangingTickFreq = 400;
    public int containerUpdateTicks = 3;
    public int hopperTransferTicks = 20;
    public int itemMergeRadius = 1;
    public int itemDespawnTicks = 3000;
    public int xpMergeRadius = 2;
    public int arrowDespawnTicks = 300;

    public int entityActivationRangeMonsters = 24;
    public int entityActivationRangeAnimals = 12;
    public int entityActivationRangeWater = 12;
    public int entityActivationRangeMisc = 8;
    public boolean entityActivationRangeVillagersWork = true;

    public int mobCapMonster = 40;
    public int mobCapAnimal = 8;
    public int mobCapWater = 10;
    public int mobCapWaterAmbient = 15;
    public int mobCapUndergroundWater = 10;

    public boolean disableFluidUpdates = false;
    public boolean disableThunder = false;
    public boolean disableIceAndSnow = false;
    public boolean disableExplosions = false;
    public boolean disableFireSpread = false;
    public boolean disableLightningFire = false;
    public boolean disableSnowMelt = false;
    public boolean disableIceMelt = false;
    public int fireTickRate = 30;
    public int maxEntityCollisions = 2;

    public boolean enableAlternateCurrent = false;
    public boolean fastLeafDecay = true;

    public int simulationDistance = 8;

    public String memoryProfile = "auto";

    private AmetystKQConfig() {}

    public static AmetystKQConfig load(final Path configDir) {
        final AmetystKQConfig config = new AmetystKQConfig();
        final Path configFile = configDir.resolve("ametystkq-config.yml");

        try {
            final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

            final CommentedConfigurationNode node;
            if (Files.notExists(configFile)) {
                node = CommentedConfigurationNode.root(loader.defaultOptions());
                config.saveDefaults(node);
                Files.createDirectories(configDir);
                loader.save(node);
                LOGGER.info("Created default ametystkq-config.yml");
            } else {
                node = loader.load();
            }

            config.disablePhantomSpawning   = node.node("disable-phantom-spawning").getBoolean(false);
            config.disablePatrolSpawning     = node.node("disable-patrol-spawning").getBoolean(false);
            config.disableWanderingTrader    = node.node("disable-wandering-trader").getBoolean(false);
            config.disableVillageSiege       = node.node("disable-village-siege").getBoolean(false);
            config.disableCatSpawner         = node.node("disable-cat-spawner").getBoolean(false);
            config.disablePillagerPatrols    = node.node("disable-pillager-patrols").getBoolean(false);
            config.disableRaid               = node.node("disable-raid").getBoolean(false);
            config.disableTrialSpawner       = node.node("disable-trial-spawner").getBoolean(false);
            config.disableBeeSpawner         = node.node("disable-bee-spawner").getBoolean(false);
            config.disableDolphinSpawner     = node.node("disable-dolphin-spawner").getBoolean(false);
            config.disableOcelotSpawner      = node.node("disable-ocelot-spawner").getBoolean(false);
            config.disableFrogSpawner        = node.node("disable-frog-spawner").getBoolean(false);

            config.autoSaveTicks         = node.node("auto-save-ticks").getInt(12000);
            config.mobSpawnerTickRate    = node.node("mob-spawner-tick-rate").getInt(4);
            config.hangingTickFreq       = node.node("hanging-tick-frequency").getInt(400);
            config.containerUpdateTicks  = node.node("container-update-ticks").getInt(3);
            config.hopperTransferTicks   = node.node("hopper-transfer-ticks").getInt(20);
            config.itemMergeRadius       = node.node("item-merge-radius").getInt(1);
            config.itemDespawnTicks      = node.node("item-despawn-ticks").getInt(3000);
            config.xpMergeRadius         = node.node("xp-merge-radius").getInt(2);
            config.arrowDespawnTicks     = node.node("arrow-despawn-ticks").getInt(300);

            config.entityActivationRangeMonsters   = node.node("entity-activation-range", "monsters").getInt(24);
            config.entityActivationRangeAnimals    = node.node("entity-activation-range", "animals").getInt(12);
            config.entityActivationRangeWater      = node.node("entity-activation-range", "water").getInt(12);
            config.entityActivationRangeMisc       = node.node("entity-activation-range", "misc").getInt(8);
            config.entityActivationRangeVillagersWork = node.node("entity-activation-range", "villagers-work").getBoolean(true);

            config.mobCapMonster          = node.node("mob-cap", "monster").getInt(40);
            config.mobCapAnimal           = node.node("mob-cap", "animal").getInt(8);
            config.mobCapWater            = node.node("mob-cap", "water").getInt(10);
            config.mobCapWaterAmbient     = node.node("mob-cap", "water-ambient").getInt(15);
            config.mobCapUndergroundWater = node.node("mob-cap", "underground-water").getInt(10);

            config.disableFluidUpdates    = node.node("disable-fluid-updates").getBoolean(false);
            config.disableThunder         = node.node("disable-thunder").getBoolean(false);
            config.disableIceAndSnow      = node.node("disable-ice-and-snow").getBoolean(false);
            config.disableExplosions      = node.node("disable-explosions").getBoolean(false);
            config.disableFireSpread      = node.node("disable-fire-spread").getBoolean(false);
            config.disableLightningFire   = node.node("disable-lightning-fire").getBoolean(false);
            config.disableSnowMelt        = node.node("disable-snow-melt").getBoolean(false);
            config.disableIceMelt         = node.node("disable-ice-melt").getBoolean(false);
            config.fireTickRate           = node.node("fire-tick-rate").getInt(30);
            config.maxEntityCollisions    = node.node("max-entity-collisions").getInt(2);

            config.enableAlternateCurrent = node.node("enable-alternate-current").getBoolean(false);
            config.fastLeafDecay          = node.node("fast-leaf-decay").getBoolean(true);

            config.simulationDistance     = node.node("simulation-distance").getInt(8);
            config.memoryProfile          = node.node("memory-profile").getString("auto");

            LOGGER.info("Loaded ametystkq-config.yml");
        } catch (final Exception e) {
            LOGGER.error("Failed to load ametystkq-config.yml, using defaults", e);
        }

        instance = config;
        return config;
    }

    private void saveDefaults(final CommentedConfigurationNode node) throws SerializationException {
        node.node("disable-phantom-spawning").comment("Disable phantom spawning").set(false);
        node.node("disable-patrol-spawning").comment("Disable pillager patrol spawning").set(false);
        node.node("disable-wandering-trader").comment("Disable wandering trader spawning").set(false);
        node.node("disable-village-siege").comment("Disable zombie village sieges").set(false);
        node.node("disable-cat-spawner").comment("Disable cat spawning in villages").set(false);
        node.node("disable-pillager-patrols").comment("Disable pillager patrols").set(false);
        node.node("disable-raid").comment("Disable all raids").set(false);
        node.node("disable-trial-spawner").comment("Disable trial spawners").set(false);
        node.node("disable-bee-spawner").comment("Disable bee nest spawning").set(false);
        node.node("disable-dolphin-spawner").comment("Disable dolphin spawning").set(false);
        node.node("disable-ocelot-spawner").comment("Disable ocelot spawning").set(false);
        node.node("disable-frog-spawner").comment("Disable frog spawning").set(false);
        node.node("auto-save-ticks").comment("Ticks between auto-saves (6000=5min, 0=disable)").set(12000);
        node.node("mob-spawner-tick-rate").comment("Ticks between mob spawner activation checks").set(4);
        node.node("hanging-tick-frequency").comment("Ticks between hanging entity updates (paintings/item frames)").set(400);
        node.node("container-update-ticks").comment("Ticks between container inventory updates").set(3);
        node.node("hopper-transfer-ticks").comment("Ticks between hopper transfers (higher = less lag)").set(20);
        node.node("item-merge-radius").comment("Item merge radius in blocks").set(1);
        node.node("item-despawn-ticks").comment("Ticks before items despawn (6000=5min)").set(3000);
        node.node("xp-merge-radius").comment("Experience orb merge radius in blocks").set(2);
        node.node("arrow-despawn-ticks").comment("Ticks before arrows despawn").set(300);
        var er = node.node("entity-activation-range");
        er.node("monsters").comment("Range to activate monster AI").set(24);
        er.node("animals").comment("Range to activate animal AI").set(12);
        er.node("water").comment("Range to activate water mob AI").set(12);
        er.node("misc").comment("Range to activate misc entity AI").set(8);
        er.node("villagers-work").comment("Allow villagers to work outside activation range").set(true);
        var mc = node.node("mob-cap");
        mc.node("monster").comment("Max monster mobs per player").set(40);
        mc.node("animal").comment("Max animal mobs per player").set(8);
        mc.node("water").comment("Max water mobs per player").set(10);
        mc.node("water-ambient").comment("Max ambient water mobs per player").set(15);
        mc.node("underground-water").comment("Max underground water mobs per player").set(10);
        node.node("disable-fluid-updates").comment("Disable all fluid flow updates").set(false);
        node.node("disable-thunder").comment("Disable all thunder/lightning").set(false);
        node.node("disable-ice-and-snow").comment("Disable ice and snow formation").set(false);
        node.node("disable-explosions").comment("Disable all explosion block damage").set(false);
        node.node("disable-fire-spread").comment("Disable fire spreading").set(false);
        node.node("disable-lightning-fire").comment("Disable lightning starting fires").set(false);
        node.node("disable-snow-melt").comment("Disable snow melting from light").set(false);
        node.node("disable-ice-melt").comment("Disable ice melting from light").set(false);
        node.node("fire-tick-rate").comment("Ticks between fire spread checks (higher = less lag)").set(30);
        node.node("max-entity-collisions").comment("Max entities that can collide with one entity").set(2);
        node.node("enable-alternate-current").comment("Use alternate current redstone (faster)").set(false);
        node.node("fast-leaf-decay").comment("Enable fast leaf decay").set(true);
        node.node("simulation-distance").comment("Chunks to simulate entities/ticking").set(8);
        node.node("memory-profile").comment("RAM tuning: auto, low (1-2GB), medium (4GB), high (8GB+)").set("auto");
    }

    public static AmetystKQConfig get() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }
}
