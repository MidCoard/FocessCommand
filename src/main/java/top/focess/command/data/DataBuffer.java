package top.focess.command.data;

import org.jetbrains.annotations.NotNull;

/**
 * A type-specific storage buffer used by {@link top.focess.command.DataCollection}.
 * <p>
 * {@code DataBuffer} follows a strict **Write-then-Read** lifecycle, similar to Java's 
 * {@link java.nio.Buffer}. During command parsing, the framework {@link #put(Object)}s 
 * values into the buffer. Once parsing is complete, the framework calls {@link #flip()}, 
 * making the values available for retrieval by the {@link top.focess.command.CommandExecutor}.
 *
 * @param <T> The type of data stored in this buffer.
 */
public abstract class DataBuffer<T> {

    /**
     * Finalizes the writing phase and prepares the buffer for reading.
     * <p>
     * After this call, internal pointers are typically reset so that {@link #get()} 
     * starts from the first element.
     */
    public abstract void flip();

    /**
     * Appends a value to the buffer.
     *
     * @param t The value to store.
     */
    public abstract void put(T t);

    /**
     * Retrieves the next available element from the buffer and advances the 
     * internal read pointer.
     *
     * @return The next element, or {@code null} if the end of the buffer is reached.
     */
    public abstract T get();

    /**
     * Retrieves an element at a specific index without advancing the internal 
     * read pointer.
     *
     * @param index The 0-based index of the element to retrieve.
     * @return The element at the specified index.
     * @throws IndexOutOfBoundsException if the index is invalid.
     */
    public abstract T get(int index);
}
