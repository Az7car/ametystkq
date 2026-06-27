package io.papermc.paper.util.pool;

import java.util.function.Supplier;

public final class ObjectPool<T> {

    private final ThreadLocal<Pool<T>> pools;
    private final Supplier<T> factory;
    private final Runnable reset;

    private ObjectPool(final Supplier<T> factory, final Runnable reset, final int maxSize) {
        this.factory = factory;
        this.reset = reset;
        this.pools = ThreadLocal.withInitial(() -> new Pool<>(factory, reset, maxSize));
    }

    public T get() {
        return this.pools.get().get();
    }

    public void free(final T instance) {
        this.pools.get().free(instance);
    }

    private static final class Pool<T> {
        private final Object[] stack;
        private final Supplier<T> factory;
        private final Runnable reset;
        private int cursor;

        Pool(final Supplier<T> factory, final Runnable reset, final int maxSize) {
            this.stack = new Object[maxSize];
            this.factory = factory;
            this.reset = reset;
            this.cursor = 0;
        }

        @SuppressWarnings("unchecked")
        T get() {
            if (this.cursor > 0) {
                return (T) this.stack[--this.cursor];
            }
            return this.factory.get();
        }

        void free(final T instance) {
            if (this.cursor < this.stack.length) {
                this.reset.run();
                this.stack[this.cursor++] = instance;
            }
        }
    }

    public static <T> ObjectPool<T> create(final Supplier<T> factory, final Runnable reset, final int maxSize) {
        return new ObjectPool<>(factory, reset, maxSize);
    }
}
