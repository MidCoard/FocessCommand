package top.focess.command.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.CharBuffer;

/**
 * Represent a buffer of String.
 */
public class StringBuffer extends DataBuffer<String> {

    private final CharBuffer[] charBuffers;
    private int pos;

    private StringBuffer(final int size) {
        this.charBuffers = new CharBuffer[size];
    }

    /**
     * Allocate a StringBuffer with fixed size
     *
     * @param size the target buffer size
     * @return a StringBuffer with fixed size
     */
    @NotNull
    @Contract("_ -> new")
    public static StringBuffer allocate(final int size) {
        return new StringBuffer(size);
    }

    @Override
    public void flip() {
        this.pos = 0;
    }

    @Override
    public void put(final String s) {
        if (this.pos < this.charBuffers.length) {
            if (s == null) {
                this.charBuffers[this.pos++] = null;
            } else {
                this.charBuffers[this.pos] = CharBuffer.allocate(s.length()).put(s);
                this.charBuffers[this.pos].flip();
                this.pos++;
            }
        }
    }

    @Override
    public String get() {
        if (this.pos < this.charBuffers.length) {
            CharBuffer cb = this.charBuffers[this.pos++];
            return cb == null ? null : new String(cb.array());
        }
        return null;
    }

    @Override
    public String get(final int index) {
        if (index >= 0 && index < this.charBuffers.length) {
            CharBuffer cb = this.charBuffers[index];
            return cb == null ? null : new String(cb.array());
        }
        return null;
    }
}
