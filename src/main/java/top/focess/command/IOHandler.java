package top.focess.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class is used to handle input and output when executing Command.
 */
public abstract class IOHandler {

    private final Object LOCK = new Object();

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
     *
     * @return the input String
     * @throws InputTimeoutException if the command has waited for more than 10 minutes to get executor input string
     */
    @NonNull
    public String input() throws InputTimeoutException {
        if (!this.flag)
            this.hasInput();
        this.flag = false;
        synchronized (LOCK) {
            if (this.value == null)
                throw new InputTimeoutException();
            // this.value cannot be null, because the change of value is synchronized
            return Objects.requireNonNull(this.value);
        }
    }

    /**
     * Used to input String
     *
     * @param input the inputted String
     */
    public void input(@Nullable final String input) {
        synchronized (LOCK) {
            this.value = input;
        }
        this.flag = true;
    }

    /**
     * Indicate there needs the MiraiCode of this input if it is a Mirai Message, or the string value of this input.
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
    public abstract boolean hasInput(boolean flag);

}
