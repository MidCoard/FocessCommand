package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class is used to handle input and output when executing Command.
 */
public abstract class IOHandler {

    /**
     * The timeout value (in milliseconds) that indicates {@link #input(long)} should wait indefinitely.
     */
    public static final long INFINITY = 0L;

    @Nullable
    protected volatile String value;
    protected volatile boolean flag;

    /**
     * Used to output String
     *
     * @param output output String
     */
    public abstract void output(String output);

    /**
     * Used to get input String
     * <p>
     * Note: if there is no input String yet, this method blocks indefinitely until one arrives.
     *
     * @return the input String
     * @throws InputTimeoutException if no input String is provided
     * @see #input(long)
     * @see #hasInput()
     */
    @NotNull
    public synchronized String input() throws InputTimeoutException {
        return this.input(INFINITY);
    }

    /**
     * Used to get input String
     * <p>
     * Note: if there is no input String yet, this method blocks until one arrives or the given timeout elapses.
     *
     * @param timeout the maximum time (in milliseconds) to wait for an input String;
     *                {@link #INFINITY} (0) means wait indefinitely
     * @return the input String
     * @throws InputTimeoutException if no input String is provided within the given timeout
     * @see #hasInput(long)
     */
    @NotNull
    public synchronized String input(final long timeout) throws InputTimeoutException {
        if (!this.flag && !this.hasInput(timeout))
            throw new InputTimeoutException();
        // one of the callers can consume the input String
        this.flag = false;
        if (this.value == null)
            throw new InputTimeoutException();
        // this.value cannot be null, because the change of value is synchronized
        return Objects.requireNonNull(this.value);
    }

    /**
     * Used to input String
     *
     * @param input the inputted String
     */
    public synchronized void input(@Nullable final String input) {
        this.value = input;
        this.flag = true;
        this.notifyAll();
    }

    /**
     * Wait indefinitely until an input String is provided.
     *
     * @return true if an input message arrived, false if the wait was interrupted
     * @see #hasInput(long)
     */
    public synchronized boolean hasInput() {
        return this.hasInput(INFINITY);
    }

    /**
     * Wait until an input String is provided or the given timeout elapses.
     *
     * @param timeout the maximum time (in milliseconds) to wait for an input String;
     *                {@link #INFINITY} (0) means wait indefinitely
     * @return true if an input message arrived before the timeout, false otherwise
     */
    public synchronized boolean hasInput(final long timeout) {
        try {
            if (timeout <= INFINITY) {
                // guard against spurious wakeups: keep waiting until input arrives
                while (!this.flag)
                    this.wait();
                return true;
            }
            final long deadline = System.currentTimeMillis() + timeout;
            // guard against spurious wakeups: keep waiting until input arrives or the deadline passes
            while (!this.flag) {
                final long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                    return false;
                this.wait(remaining);
            }
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
