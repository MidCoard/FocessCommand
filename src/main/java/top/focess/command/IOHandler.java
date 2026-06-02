package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class is used to handle input and output when executing Command.
 */
public abstract class IOHandler {

    /**
     * The maximum time (in milliseconds) {@link #input()} waits for an input String before timing out.
     */
    public static final long INPUT_TIMEOUT_MILLIS = 10 * 60 * 1000L;

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
     * Note: if there is no input String yet, this method blocks until one arrives or
     * {@link #INPUT_TIMEOUT_MILLIS} elapses.
     *
     * @return the input String
     * @throws InputTimeoutException if no input String is provided within {@link #INPUT_TIMEOUT_MILLIS}
     * @see #hasInput()
     */
    @NotNull
    public synchronized String input() throws InputTimeoutException {
        if (!this.flag && !this.hasInput())
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
     * Wait until an input String is provided or the input times out.
     *
     * @return true if an input message arrived before the timeout, false otherwise
     */
    public synchronized boolean hasInput() {
        final long deadline = System.currentTimeMillis() + INPUT_TIMEOUT_MILLIS;
        try {
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
