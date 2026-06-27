package io.papermc.paper.util.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

public final class FastEventExecutor implements EventExecutor {

    private static final int MAX_EVENT_CLASSES = 4096;
    private static final AtomicReferenceArray<HandlerRegistration[]> handlerCache = new AtomicReferenceArray<>(MAX_EVENT_CLASSES);

    private final Listener listener;
    private final Consumer<Event> handler;
    private final Plugin plugin;
    private final EventPriority priority;
    private final boolean ignoreCancelled;

    public FastEventExecutor(final Listener listener, final Consumer<Event> handler,
                              final Plugin plugin, final EventPriority priority,
                              final boolean ignoreCancelled) {
        this.listener = listener;
        this.handler = handler;
        this.plugin = plugin;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
    }

    @Override
    public void execute(final Listener listener, final Event event) throws EventException {
        if (this.ignoreCancelled && event instanceof Cancellable cancellable && cancellable.isCancelled()) return;
        try {
            this.handler.accept(event);
        } catch (final Exception e) {
            throw new EventException(e);
        }
    }

    public Listener listener() { return this.listener; }
    public Plugin plugin() { return this.plugin; }
    public EventPriority priority() { return this.priority; }
    public boolean ignoreCancelled() { return this.ignoreCancelled; }

    public static void cacheHandlers(final Class<? extends Event> eventClass, final HandlerRegistration[] handlers) {
        final int index = eventClass.getName().hashCode() & (MAX_EVENT_CLASSES - 1);
        handlerCache.set(index, handlers);
    }

    public static HandlerRegistration[] getCachedHandlers(final Class<? extends Event> eventClass) {
        final int index = eventClass.getName().hashCode() & (MAX_EVENT_CLASSES - 1);
        return handlerCache.get(index);
    }

    public record HandlerRegistration(FastEventExecutor executor, EventPriority priority, Plugin plugin) {}
}
