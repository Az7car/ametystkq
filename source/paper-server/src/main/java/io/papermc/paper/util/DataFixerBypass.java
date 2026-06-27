package io.papermc.paper.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DataFixerBypass {

    private static final Logger LOGGER = LoggerFactory.getLogger("DataFixerBypass");
    private static final Set<String> bypassedWorlds = new CopyOnWriteArraySet<>();
    private static boolean globallyDisabled = false;

    private DataFixerBypass() {}

    public static void setGloballyDisabled(final boolean disabled) {
        globallyDisabled = disabled;
        if (disabled) {
            LOGGER.info("DataFixerUpper globally disabled. Save RAM: ~500MB-2GB");
        }
    }

    public static boolean shouldBypass(final LevelStorageSource.LevelDirectory levelDir) {
        if (globallyDisabled) return true;
        return bypassedWorlds.contains(levelDir.path().getFileName().toString());
    }

    public static void markBypass(final String worldName) {
        bypassedWorlds.add(worldName);
    }

    public static DataFixer wrapDataFixer(final DataFixer original) {
        if (globallyDisabled) return new NoopDataFixer();
        return new BypassAwareDataFixer(original);
    }

    private static final class NoopDataFixer implements DataFixer {
        @Override
        public <T> Dynamic<T> update(final com.mojang.datafixers.DSL.TypeReference type,
                                      final Dynamic<T> input, final int version, final int newVersion) {
            return input;
        }

        @Override
        public Schema getSchema(final int key) {
            return null;
        }
    }

    private static final class BypassAwareDataFixer implements DataFixer {
        private final DataFixer delegate;

        BypassAwareDataFixer(final DataFixer delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> Dynamic<T> update(final com.mojang.datafixers.DSL.TypeReference type,
                                      final Dynamic<T> input, final int version, final int newVersion) {
            if (globallyDisabled || version >= newVersion) return input;
            return this.delegate.update(type, input, version, newVersion);
        }

        @Override
        public Schema getSchema(final int key) {
            return this.delegate.getSchema(key);
        }
    }
}
