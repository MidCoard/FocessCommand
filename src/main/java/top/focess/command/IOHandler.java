package top.focess.command;

import org.checkerframework.checker.nullness.qual.NonNull;
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
    @NonNull
    public synchronized  String input() throws InputTimeoutException {
        // one of the callers can get the input String
        if (this.flag) {
            this.flag = false;
            if (this.value == null)
                throw new InputTimeoutException();
            // this.value cannot be null, because the change of value is synchronized
            return Objects.requireNonNull(this.value);
        } else {
            if (this.hasInput()) {
                this.flag = false;
                if (this.value == null)
                    throw new InputTimeoutException();
                // this.value cannot be null, because the change of value is synchronized
                return Objects.requireNonNull(this.value);
            } else throw new InputTimeoutException();
        }
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
     * Indicate there needs the MiraiCode of this input if it is a Mirai Message, or the string value of this input.
     *
     * Note: this method should not be a blocking method
     *
     * @return true if there is an input String, false otherwise
     * @see #hasInput(boolean)
     */
    public boolean hasInput() {
        return this.hasInput(false);
    }

    /**
     * Indicate there needs an input String.
     *
     * @param flag true if you need the MiraiCode of this input when it is a Mirai Message, false if you need the string value of this input
     * @return true if there is an input String, false otherwise
     */
    public synchronized boolean hasInput(boolean flag) {
        try {
            this.wait();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

}
