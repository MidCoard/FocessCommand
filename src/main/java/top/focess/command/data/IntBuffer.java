package top.focess.command.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represent a buffer of Int.
 */
public class IntBuffer extends DataBuffer<Integer> {

    private final java.nio.IntBuffer buffer;

    private IntBuffer(final int size) {
        this.buffer = java.nio.IntBuffer.allocate(size);
    }

    /**
     * Allocate a IntBuffer with fixed size
     *
     * @param size the target buffer size
     * @return a IntBuffer with fixed size
     */
    @NotNull
    @Contract("_ -> new")
    public static IntBuffer allocate(final int size) {
        return new IntBuffer(size);
    }

    @Override
    public void flip() {
        this.buffer.flip();
    }

    @Override
    public void put(final Integer integer) {
        this.buffer.put(integer);
    }

    @NotNull
    @Override
    public Integer get() {
        return this.buffer.get();
    }

    @NotNull
    @Override
    public Integer get(final int index) {
        return this.buffer.get(index);
    }
}
