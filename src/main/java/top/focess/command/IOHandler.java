package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class is used to handle input and output when executing Command.
 */
public abstract class IOHandler {

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
     * Note: if there is no input String, this method will call {@link #hasInput()} and wait until there is an input String
     * @return the input String
     * @throws InputTimeoutException if the command has waited for more than the time it expects
     * @see #hasInput()
     */
    @NotNull
    public synchronized String input() throws InputTimeoutException {
        // If no input is ready yet, wait until one arrives (or the wait is interrupted).
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
        this.notify();
    }

    /**
     * Indicate there needs a message.
     *
     * @return true if there is an input message, false otherwise
     */
    public synchronized boolean hasInput() {
        try {
            // guard against spurious wakeups: only return once an input has actually been provided
            while (!this.flag)
                this.wait();
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
