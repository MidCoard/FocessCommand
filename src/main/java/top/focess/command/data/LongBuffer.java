package top.focess.command.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represent a buffer of Long.
 */
public class LongBuffer extends DataBuffer<Long> {

    private final java.nio.LongBuffer buffer;

    private LongBuffer(final int size) {
        this.buffer = java.nio.LongBuffer.allocate(size);
    }

    /**
     * Allocate a LongBuffer with fixed size
     *
     * @param size the target buffer size
     * @return a LongBuffer with fixed size
     */
    @NotNull
    @Contract("_ -> new")
    public static LongBuffer allocate(final int size) {
        return new LongBuffer(size);
    }

    @Override
    public void flip() {
        this.buffer.flip();
    }

    @Override
    public void put(final Long l) {
        this.buffer.put(l);
    }

    @NotNull
    @Override
    public Long get() {
        return this.buffer.get();
    }

    @NotNull
    @Override
    public Long get(final int index) {
        return this.buffer.get(index);
    }
}
