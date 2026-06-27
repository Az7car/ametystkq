package io.papermc.paper.util.event;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import java.util.*;
import java.util.function.Consumer;

public final class FastEventBus {

    private final Map<Class<? extends Event>, HandlerList> handlerLists = new HashMap<>();
    private final Object lock = new Object();

    public void register(final Class<? extends Event> eventClass, final FastEventExecutor executor) {
        final HandlerList handlerList = this.getOrCreateHandlerList(eventClass);
        handlerList.register(new RegisteredListener(
            executor.listener(),
            executor,
            executor.priority(),
            executor.plugin(),
            executor.ignoreCancelled()
        ));
    }

    public void register(final Class<? extends Event> eventClass, final Listener listener,
                          final EventPriority priority, final Plugin plugin,
                          final boolean ignoreCancelled, final Consumer<Event> handler) {
        final FastEventExecutor executor = new FastEventExecutor(listener, handler, plugin, priority, ignoreCancelled);
        this.register(eventClass, executor);
    }

    public void unregister(final Plugin plugin) {
        synchronized (this.lock) {
            for (final HandlerList list : this.handlerLists.values()) {
                list.unregister(plugin);
            }
        }
    }

    public void unregister(final Listener listener) {
        synchronized (this.lock) {
            for (final HandlerList list : this.handlerLists.values()) {
                list.unregister(listener);
            }
        }
    }

    public Event callEvent(final Event event) {
        final HandlerList handlers = event.getHandlers();
        if (handlers == null) return event;

        final RegisteredListener[] listeners = handlers.getRegisteredListeners();
        if (listeners == null || listeners.length == 0) return event;

        for (final RegisteredListener listener : listeners) {
            if (listener.getPriority() == EventPriority.MONITOR) continue;
            if (listener.isIgnoringCancelled() && event instanceof Cancellable cancellable && cancellable.isCancelled()) continue;
            try {
                listener.callEvent(event);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return event;
    }

    public void callMonitor(final Event event) {
        final HandlerList handlers = event.getHandlers();
        if (handlers == null) return;

        final RegisteredListener[] listeners = handlers.getRegisteredListeners();
        if (listeners == null || listeners.length == 0) return;

        for (final RegisteredListener listener : listeners) {
            if (listener.getPriority() == EventPriority.MONITOR) {
                try {
                    listener.callEvent(event);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private HandlerList getOrCreateHandlerList(final Class<? extends Event> eventClass) {
        synchronized (this.lock) {
            return this.handlerLists.computeIfAbsent(eventClass, k -> new HandlerList());
        }
    }
}
