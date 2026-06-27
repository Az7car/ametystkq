package io.papermc.paper.util.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class LockFreeCraftScheduler implements BukkitScheduler {

    private static final int TASK_QUEUE_SIZE = 65536;
    private static final int WORKER_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

    private final AtomicReferenceArray<TaskHolder> pendingTasks = new AtomicReferenceArray<>(TASK_QUEUE_SIZE);
    private final AtomicInteger pendingHead = new AtomicInteger();
    private final AtomicInteger pendingTail = new AtomicInteger();

    private final ObjectArrayList<TaskHolder> syncTasks = new ObjectArrayList<>();
    private final AtomicInteger syncTaskId = new AtomicInteger(1);

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
        WORKER_THREADS,
        new ThreadFactoryBuilder().setNameFormat("AmetystKQ Scheduler Worker - %d").setDaemon(true).build()
    );

    private final ConcurrentHashMap<Integer, Future<?>> asyncTaskFutures = new ConcurrentHashMap<>();
    private final AtomicLong currentTick = new AtomicLong();

    @Override
    public int scheduleSyncDelayedTask(@NotNull final Plugin plugin, @NotNull final Runnable task) {
        return this.scheduleSyncDelayedTask(plugin, task, 0);
    }

    @Override
    public int scheduleSyncDelayedTask(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task) {
        return this.scheduleSyncDelayedTask(plugin, task, 0);
    }

    @Override
    public int scheduleSyncDelayedTask(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay) {
        return this.addSyncTask(plugin, task, delay, 0);
    }

    @Override
    public int scheduleSyncDelayedTask(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay) {
        return this.addSyncTask(plugin, task, delay, 0);
    }

    @Override
    public int scheduleSyncRepeatingTask(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay, final long period) {
        return this.addSyncTask(plugin, task, delay, period);
    }

    @Override
    public int scheduleSyncRepeatingTask(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay, final long period) {
        return this.addSyncTask(plugin, task, delay, period);
    }

    @Override
    public int scheduleAsyncDelayedTask(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay) {
        return this.addAsyncTask(plugin, task, delay, 0);
    }

    @Override
    public int scheduleAsyncRepeatingTask(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay, final long period) {
        return this.addAsyncTask(plugin, task, delay, period);
    }

    @Override
    public <T> Future<T> callSyncMethod(@NotNull final Plugin plugin, @NotNull final Callable<T> task) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        this.addSyncTask(plugin, () -> {
            try {
                future.complete(task.call());
            } catch (final Exception e) {
                future.completeExceptionally(e);
            }
        }, 0, 0);
        return future;
    }

    @Override
    public void cancelTask(final int taskId) {
        synchronized (this.syncTasks) {
            this.syncTasks.removeIf(t -> t.id == taskId);
        }
        final Future<?> future = this.asyncTaskFutures.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void cancelTasks(@NotNull final Plugin plugin) {
        synchronized (this.syncTasks) {
            this.syncTasks.removeIf(t -> t.plugin.equals(plugin));
        }
    }

    @Override
    public boolean isCurrentlyRunning(final int taskId) {
        return false;
    }

    @Override
    public boolean isQueued(final int taskId) {
        synchronized (this.syncTasks) {
            return this.syncTasks.stream().anyMatch(t -> t.id == taskId);
        }
    }

    @Override
    @NotNull
    public List<BukkitWorker> getActiveWorkers() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public List<BukkitTask> getPendingTasks() {
        synchronized (this.syncTasks) {
            return Collections.unmodifiableList(new ObjectArrayList<>(this.syncTasks));
        }
    }

    @Override
    @NotNull
    public BukkitTask runTask(@NotNull final Plugin plugin, @NotNull final Runnable task) throws IllegalArgumentException {
        final int id = this.addSyncTask(plugin, task, 0, 0);
        return new LockFreeTask(id, plugin, false, false);
    }

    @Override
    @NotNull
    public BukkitTask runTask(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task) throws IllegalArgumentException {
        return this.runTask(plugin, (Runnable) task);
    }

    @Override
    @NotNull
    public BukkitTask runTaskAsynchronously(@NotNull final Plugin plugin, @NotNull final Runnable task) {
        final int id = this.addAsyncTask(plugin, task, 0, 0);
        return new LockFreeTask(id, plugin, true, false);
    }

    @Override
    @NotNull
    public BukkitTask runTaskAsynchronously(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task) {
        return this.runTaskAsynchronously(plugin, (Runnable) task);
    }

    @Override
    @NotNull
    public BukkitTask runTaskLater(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay) throws IllegalArgumentException {
        final int id = this.addSyncTask(plugin, task, delay, 0);
        return new LockFreeTask(id, plugin, false, false);
    }

    @Override
    @NotNull
    public BukkitTask runTaskLater(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay) throws IllegalArgumentException {
        return this.runTaskLater(plugin, (Runnable) task, delay);
    }

    @Override
    @NotNull
    public BukkitTask runTaskLaterAsynchronously(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay) {
        final int id = this.addAsyncTask(plugin, task, delay, 0);
        return new LockFreeTask(id, plugin, true, false);
    }

    @Override
    @NotNull
    public BukkitTask runTaskLaterAsynchronously(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay) {
        return this.runTaskLaterAsynchronously(plugin, (Runnable) task, delay);
    }

    @Override
    @NotNull
    public BukkitTask runTaskTimer(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay, final long period) throws IllegalArgumentException {
        final int id = this.addSyncTask(plugin, task, delay, period);
        return new LockFreeTask(id, plugin, false, true);
    }

    @Override
    @NotNull
    public BukkitTask runTaskTimer(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay, final long period) throws IllegalArgumentException {
        return this.runTaskTimer(plugin, (Runnable) task, delay, period);
    }

    @Override
    @NotNull
    public BukkitTask runTaskTimerAsynchronously(@NotNull final Plugin plugin, @NotNull final Runnable task, final long delay, final long period) {
        final int id = this.addAsyncTask(plugin, task, delay, period);
        return new LockFreeTask(id, plugin, true, true);
    }

    @Override
    @NotNull
    public BukkitTask runTaskTimerAsynchronously(@NotNull final Plugin plugin, @NotNull final BukkitRunnable task, final long delay, final long period) {
        return this.runTaskTimerAsynchronously(plugin, (Runnable) task, delay, period);
    }

    public void mainThreadHeartbeat() {
        final long tick = this.currentTick.incrementAndGet();
        final List<TaskHolder> toExecute = new ObjectArrayList<>();
        synchronized (this.syncTasks) {
            final var it = this.syncTasks.iterator();
            while (it.hasNext()) {
                final TaskHolder task = it.next();
                if (tick >= task.nextExecutionTick) {
                    toExecute.add(task);
                    if (task.period > 0) {
                        task.nextExecutionTick = tick + task.period;
                    } else {
                        it.remove();
                    }
                }
            }
        }

        for (int i = 0; i < toExecute.size(); i++) {
            final TaskHolder task = toExecute.get(i);
            try {
                task.runnable.run();
            } catch (final Exception e) {
                MinecraftServer.LOGGER.error("Task {} for plugin {} threw exception", task.id, task.plugin.getName(), e);
            }
        }
    }

    public void cancelAllTasks() {
        synchronized (this.syncTasks) {
            this.syncTasks.clear();
        }
    }

    private int addSyncTask(final Plugin plugin, final Runnable runnable, final long delay, final long period) {
        final int id = this.syncTaskId.getAndIncrement();
        final long execTick = this.currentTick.get() + delay;
        final TaskHolder task = new TaskHolder(id, plugin, runnable, execTick, period);
        synchronized (this.syncTasks) {
            this.syncTasks.add(task);
        }
        return id;
    }

    private int addAsyncTask(final Plugin plugin, final Runnable runnable, final long delay, final long period) {
        final int id = this.syncTaskId.getAndIncrement();
        final CancellableTask cancellable = new CancellableTask();
        final Future<?> future = this.asyncExecutor.submit(() -> {
            try {
                if (delay > 0) {
                    Thread.sleep(delay * 50);
                }
                if (cancellable.isCancelled()) return;
                runnable.run();
                while (period > 0 && !cancellable.isCancelled()) {
                    Thread.sleep(period * 50);
                    if (cancellable.isCancelled()) return;
                    runnable.run();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                MinecraftServer.LOGGER.error("Async task {} for plugin {} threw exception", id, plugin.getName(), e);
            } finally {
                this.asyncTaskFutures.remove(id);
            }
        });
        this.asyncTaskFutures.put(id, future);
        return id;
    }

    @Override
    public int scheduleAsyncDelayedTask(@NotNull final Plugin plugin, @NotNull final Runnable task) {
        return this.scheduleAsyncDelayedTask(plugin, task, 0);
    }

    @Override
    public void runTask(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task) throws IllegalArgumentException {
        final int id = this.addSyncTask(plugin, wrapConsumer(plugin, task, false), 0, 0);
    }

    @Override
    public void runTaskAsynchronously(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task) {
        this.addAsyncTask(plugin, wrapConsumer(plugin, task, true), 0, 0);
    }

    @Override
    public void runTaskLater(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task, final long delay) throws IllegalArgumentException {
        this.addSyncTask(plugin, wrapConsumer(plugin, task, false), delay, 0);
    }

    @Override
    public void runTaskLaterAsynchronously(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task, final long delay) {
        this.addAsyncTask(plugin, wrapConsumer(plugin, task, true), delay, 0);
    }

    @Override
    public void runTaskTimer(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task, final long delay, final long period) throws IllegalArgumentException {
        this.addSyncTask(plugin, wrapConsumer(plugin, task, false), delay, period);
    }

    @Override
    public void runTaskTimerAsynchronously(@NotNull final Plugin plugin, @NotNull final Consumer<? super BukkitTask> task, final long delay, final long period) {
        this.addAsyncTask(plugin, wrapConsumer(plugin, task, true), delay, period);
    }

    @Override
    @NotNull
    public java.util.concurrent.Executor getMainThreadExecutor(@NotNull final Plugin plugin) {
        return command -> runTask(plugin, command);
    }

    private static Runnable wrapConsumer(final Plugin plugin, final Consumer<? super BukkitTask> consumer, final boolean async) {
        return () -> consumer.accept(new LockFreeTask(-1, plugin, async, false));
    }

    public void shutdown() {
        this.asyncExecutor.shutdown();
    }

    private static final class CancellableTask {
        private volatile boolean cancelled;

        public boolean isCancelled() { return this.cancelled; }
        public void cancel() { this.cancelled = true; }
    }

    private static final class TaskHolder implements BukkitTask {
        final int id;
        final Plugin plugin;
        final Runnable runnable;
        long nextExecutionTick;
        final long period;

        TaskHolder(final int id, final Plugin plugin, final Runnable runnable, final long nextExecutionTick, final long period) {
            this.id = id;
            this.plugin = plugin;
            this.runnable = runnable;
            this.nextExecutionTick = nextExecutionTick;
            this.period = period;
        }

        @Override
        public int getTaskId() { return this.id; }

        @Override
        public Plugin getOwner() { return this.plugin; }

        @Override
        public boolean isSync() { return this.period >= 0; }

        @Override
        public boolean isCancelled() { return false; }

        @Override
        public void cancel() {}
    }

    private record LockFreeTask(int id, Plugin plugin, boolean async, boolean repeating) implements BukkitTask {
        @Override
        public int getTaskId() { return this.id; }

        @Override
        public Plugin getOwner() { return this.plugin; }

        @Override
        public boolean isSync() { return !this.async; }

        @Override
        public boolean isCancelled() { return false; }

        @Override
        public void cancel() {}
    }
}
