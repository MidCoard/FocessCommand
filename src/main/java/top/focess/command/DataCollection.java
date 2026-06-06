package top.focess.command;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.command.data.*;
import top.focess.command.data.StringBuffer;

import java.util.Map;

/**
 * A type-safe, multi-keyed container for parsed command arguments.
 * <p>
 * {@code DataCollection} is provided to {@link CommandExecutor}s and contains all arguments 
 * matched for the current execution path. It allows for flexible data retrieval using 
 * three different strategies:
 * 
 * <h2>Retrieval Strategies</h2>
 * <ul>
 *   <li><b>Positional/Typed:</b> Retrieve the next available argument of a specific type 
 *       (e.g., {@link #getInt()}, {@link #get(Class)}). This is stateful; each call 
 *       advances an internal pointer for that specific type.</li>
 *   <li><b>Indexed:</b> Retrieve an argument of a specific type at a specific position 
 *       within its type-buffer (e.g., {@link #get(Class, int)}).</li>
 *   <li><b>Named:</b> Retrieve an argument by the name assigned via 
 *       {@link CommandArgument#named(String)} (e.g., {@link #get(String)}).</li>
 * </ul>
 */
public class DataCollection {

    private static final Map<DataConverter<?>, BufferGetter> DATA_CONVERTER_BUFFER_MAP = Maps.newConcurrentMap();

    static {
        register(DataConverter.LONG_DATA_CONVERTER, LongBuffer::allocate);
        register(DataConverter.DEFAULT_DATA_CONVERTER, StringBuffer::allocate);
        register(DataConverter.INTEGER_DATA_CONVERTER, IntBuffer::allocate);
        register(DataConverter.DOUBLE_DATA_CONVERTER, DoubleBuffer::allocate);
        register(DataConverter.BOOLEAN_DATA_CONVERTER, BooleanBuffer::allocate);
    }

    private final Map<Class<?>, DataBuffer> buffers = Maps.newHashMap();

    private final Map<String, Object> namedValues = Maps.newHashMap();

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
     * @throws IllegalStateException if the buffer is not registered
     */
    @Nullable
    public String get() {
        return this.get(String.class);
    }

    /**
     * Get int argument in order
     *
     * @return the int argument in order
     * @throws NullPointerException if the value is null
     * @throws IllegalStateException if the buffer is not registered
     */
    public int getInt() {
        final Integer value = this.get(Integer.class);
        if (value == null)
            throw new NullPointerException("Integer argument is null");
        return value;
    }

    /**
     * Get double argument in order
     *
     * @return the double argument in order
     * @throws NullPointerException if the value is null
     * @throws IllegalStateException if the buffer is not registered
     */
    public double getDouble() {
        final Double value = this.get(Double.class);
        if (value == null)
            throw new NullPointerException("Double argument is null");
        return value;
    }

    /**
     * Get boolean argument in order
     *
     * @return the boolean argument in order
     * @throws NullPointerException if the value is null
     * @throws IllegalStateException if the buffer is not registered
     */
    public boolean getBoolean() {
        final Boolean value = this.get(Boolean.class);
        if (value == null)
            throw new NullPointerException("Boolean argument is null");
        return value;
    }

    /**
     * Get long argument in order
     *
     * @return the long argument in order
     * @throws NullPointerException if the value is null
     * @throws IllegalStateException if the buffer is not registered
     */
    public long getLong() {
        final Long value = this.get(Long.class);
        if (value == null)
            throw new NullPointerException("Long argument is null");
        return value;
    }

    /**
     * Get buffer element
     *
     * @param cls the buffer elements' class
     * @param t   the default value
     * @param <T> the buffer elements' type
     * @return the buffer element, or the default value if the buffer element is null
     * @throws IllegalStateException if the buffer is not registered
     */
    @SuppressWarnings("unchecked")
    @Contract("_,!null->!null")
    public <T> T getOrDefault(final Class<T> cls, final T t) {
        final DataBuffer<?> buffer = this.buffers.get(cls);
        if (buffer == null)
            throw new IllegalStateException("Buffer not registered for class: " + cls.getName());
        final T value = (T) buffer.get();
        return value != null ? value : t;
    }

    /**
     * Get buffer element
     *
     * @param cls   the buffer elements' class
     * @param t     the default value
     * @param index the buffer element index
     * @param <T>   the buffer elements' type
     * @return the buffer element, or the default value if the buffer element is null
     * @throws IllegalStateException if the buffer is not registered
     */
    @SuppressWarnings("unchecked")
    @Contract("_,_,!null->!null")
    public <T> T getOrDefault(final Class<T> cls, final int index, final T t) {
        final DataBuffer<?> buffer = this.buffers.get(cls);
        if (buffer == null)
            throw new IllegalStateException("Buffer not registered for class: " + cls.getName());
        final T value = (T) buffer.get(index);
        return value != null ? value : t;
    }

    <T> void write(final Class<T> cls, final T t) {
        this.buffers.compute(cls, (key, value) -> {
            if (value == null)
                throw new IllegalStateException("Buffer not registered for class: " + cls.getName());
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
     * @throws IllegalStateException if the buffer is not registered
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(final Class<T> c) {
        if (this.buffers.get(c) == null)
            throw new IllegalStateException("Buffer not registered for class: " + c.getName());
        return (T) this.buffers.get(c).get();
    }

    /**
     * Get buffer element
     *
     * @param index the buffer element index
     * @param c     the buffer elements' class
     * @param <T>   the buffer elements' type
     * @return the buffer element
     * @throws IllegalStateException if the buffer is not registered
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(final Class<T> c, final int index) {
        if (this.buffers.get(c) == null)
            throw new IllegalStateException("Buffer not registered for class: " + c.getName());
        return (T) this.buffers.get(c).get(index);
    }

    void writeNamed(@NotNull final String name, final Object value) {
        this.namedValues.put(name, value);
    }

    /**
     * Get a parsed argument by the name assigned via {@link CommandArgument#named(String)}.
     *
     * @param name the name of the argument
     * @param <T>  the argument type
     * @return the named argument value, or null if it was null or not found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(@NotNull final String name) {
        return (T) this.namedValues.get(name);
    }

    /**
     * Get a parsed argument by name, falling back to a default value if absent.
     *
     * @param name the name of the argument
     * @param t    the default value
     * @param <T>  the argument type
     * @return the named argument value, or the default value if no argument has that name
     */
    @SuppressWarnings("unchecked")
    @Contract("_,!null->!null")
    public <T> T getOrDefault(@NotNull final String name, final T t) {
        if (!this.namedValues.containsKey(name))
            return t;
        return (T) this.namedValues.get(name);
    }

    /**
     * Represents a getter for buffer.

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
