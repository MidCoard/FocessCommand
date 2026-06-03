package top.focess.command.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Represent a buffer of Boolean.
 */
public class BooleanBuffer extends DataBuffer<Boolean> {

    private final ByteBuffer byteBuffer;

    private BooleanBuffer(final int size) {
        this.byteBuffer = ByteBuffer.allocate(size);
    }

    /**
     * Allocate a BooleanBuffer with fixed size
     *
     * @param size the target buffer size
     * @return a BooleanBuffer with fixed size
     */
    @NotNull
    @Contract("_ -> new")
    public static BooleanBuffer allocate(final int size) {
        return new BooleanBuffer(size);
    }

    @Override
    public void put(final Boolean b) {
        this.byteBuffer.put((byte) (b == null ? 2 : (b ? 1 : 0)));
    }

    @Override
    public Boolean get() {
        byte b = this.byteBuffer.get();
        return b == 2 ? null : b != 0;
    }

    @Override
    public Boolean get(final int index) {
        byte b = this.byteBuffer.get(index);
        return b == 2 ? null : b != 0;
    }

    @Override
    public void flip() {
        this.byteBuffer.flip();
    }
}
