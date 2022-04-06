package top.focess.command;

import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import top.focess.command.data.*;
import top.focess.command.data.StringBuffer;

import java.util.Map;
import java.util.Objects;

/**
 * Store and parser arguments for better CommandExecutor usage.
 */
public class DataCollection {

    static {
        register(DataConverter.LONG_DATA_CONVERTER, LongBuffer::allocate);
        register(DataConverter.DEFAULT_DATA_CONVERTER, StringBuffer::allocate);
        register(DataConverter.INTEGER_DATA_CONVERTER, IntBuffer::allocate);
        register(DataConverter.DOUBLE_DATA_CONVERTER, DoubleBuffer::allocate);
        register(DataConverter.BOOLEAN_DATA_CONVERTER, BooleanBuffer::allocate);
    }

    private static final Map<DataConverter<?>, BufferGetter> DATA_CONVERTER_BUFFER_MAP = Maps.newConcurrentMap();
    private final Map<Class<?>, DataBuffer> buffers = Maps.newHashMap();

    /**
     * Initialize the DataCollection with fixed size.
     *
     * @param dataConverters the data converters
     */
    public DataCollection(@NotNull final DataConverter<?>[] dataConverters) {
        final Map<DataConverter<?>, Integer> map = Maps.newHashMap();
        for (final DataConverter<?> dataConverter : dataConverters)
            map.compute(dataConverter, (k, v) -> {
                if (v == null)
                    v = 0;
                v++;
                return v;
            });
        for (final DataConverter<?> dataConverter : map.keySet())
            this.buffers.put(dataConverter.getTargetClass(), DATA_CONVERTER_BUFFER_MAP.get(dataConverter).newBuffer(map.get(dataConverter)));
    }

    /**
     * Register the getter of the buffer
     *
     * @param dataConverter the buffer data converter
     * @param bufferGetter  the getter of the buffer
     */
    public static void register(final DataConverter<?> dataConverter, final BufferGetter bufferGetter) {
        DATA_CONVERTER_BUFFER_MAP.put(dataConverter, bufferGetter);
    }

    /**
     * Unregister the getter of the buffer
     * @param dataConverter the data converter
     */
    public static void unregister(final DataConverter<?> dataConverter) {
        DATA_CONVERTER_BUFFER_MAP.remove(dataConverter);
    }

    /**
     * Unregister all the getter of the buffers
     */
    public static void unregisterAll() {
        DATA_CONVERTER_BUFFER_MAP.clear();
    }

    /**
     * Flip all the buffers. Make them all readable.
     */
    void flip() {
        for (final Class<?> c : this.buffers.keySet())
            this.buffers.get(c).flip();
    }

    /**
     * Get String argument in order
     *
     * @return the String argument in order
     * @throws NullPointerException if the value is null
     */
    @NonNull
    public String get() {
        return Objects.requireNonNull(this.get(String.class));
    }

    /**
     * Get int argument in order
     *
     * @return the int argument in order
     * @throws NullPointerException if the value is null
     */
    public int getInt() {
        return Objects.requireNonNull(this.get(Integer.class));
    }

    /**
     * Get double argument in order
     *
     * @return the double argument in order
     * @throws NullPointerException if the value is null
     */
    public double getDouble() {
        return Objects.requireNonNull(this.get(Double.class));
    }

    /**
     * Get boolean argument in order
     *
     * @return the boolean argument in order
     * @throws NullPointerException if the value is null
     */
    public boolean getBoolean() {
        return Objects.requireNonNull(this.get(Boolean.class));
    }

    /**
     * Get long argument in order
     *
     * @return the long argument in order
     * @throws NullPointerException if the value is null
     */
    public long getLong() {
        return Objects.requireNonNull(this.get(Long.class));
    }

    /**
     * Get buffer element
     *
     * @param cls the buffer elements' class
     * @param t   the default value
     * @param <T> the buffer elements' type
     * @return the buffer element
     * @throws UnsupportedOperationException if the buffer is not registered
     */
    @Contract("_,!null->!null")
    public <T> T getOrDefault(final Class<T> cls, final T t) {
        try {
            if (this.buffers.get(cls) == null)
                throw new UnsupportedOperationException();
            return (T) this.buffers.get(cls).get();
        } catch (final Exception e) {
            return t;
        }
    }

    /**
     * Get buffer element
     *
     * @param cls   the buffer elements' class
     * @param t     the default value
     * @param index the buffer element index
     * @param <T>   the buffer elements' type
     * @return the buffer element
     * @throws UnsupportedOperationException if the buffer is not registered
     */
    @Contract("_,_,!null->!null")
    public <T> T getOrDefault(final Class<T> cls, final int index, final T t) {
        try {
            if (this.buffers.get(cls) == null)
                throw new UnsupportedOperationException();
            return (T) this.buffers.get(cls).get(index);
        } catch (final Exception e) {
            return t;
        }
    }

    <T> void write(final Class<T> cls, final T t) {
        this.buffers.compute(cls, (key, value) -> {
            if (value == null)
                throw new UnsupportedOperationException();
            value.put(t);
            return value;
        });
    }

    /**
     * Get buffer element
     *
     * @param c   the buffer elements' class
     * @param <T> the buffer elements' type
     * @return the buffer element
     * @throws UnsupportedOperationException if the buffer is not registered
     */
    @NotNull
    public <T> T get(final Class<T> c) {
        if (this.buffers.get(c) == null)
            throw new UnsupportedOperationException();
        return (T) this.buffers.get(c).get();
    }

    /**
     * Get buffer element
     *
     * @param index the buffer element index
     * @param c     the buffer elements' class
     * @param <T>   the buffer elements' type
     * @return the buffer element
     * @throws UnsupportedOperationException if the buffer is not registered
     */
    @NotNull
    public <T> T get(final Class<T> c, final int index) {
        if (this.buffers.get(c) == null)
            throw new UnsupportedOperationException();
        return (T) this.buffers.get(c).get(index);
    }

    /**
     * Represents a getter for buffer.
     *
     * This is a functional interface whose functional method is {@link BufferGetter#newBuffer(int)}.
     */
    @FunctionalInterface
    public interface BufferGetter {
        /**
         * Instance a buffer with fixed size
         *
         * @param size the initialized size of the buffer
         * @return the buffer
         */
        DataBuffer<?> newBuffer(int size);
    }
}
