package top.focess.command;

import org.jetbrains.annotations.NotNull;

/**
 * The result after executing a Command.
 */
public enum CommandResult {
    /**
     * It is accepted by the CommandExecutor
     */
    ALLOW(1),
    /**
     * It is not accepted by the CommandExecutor
     */
    REFUSE(2),
    /**
     * It is not accepted by the Command
     */
    COMMAND_REFUSED(4),
    /**
     * It indicates that print help information to the receiver
     */
    ARGS(8),
    /**
     * It indicates that print help information to the receiver and the command is executed
     */
    ARGS_EXECUTED(16),
    /**
     * It indicates that there is an exception
     */
    REFUSE_EXCEPTION(32),
    /**
     * It includes all CommandResult
     */
    ALL(ALLOW, REFUSE, COMMAND_REFUSED, ARGS, ARGS_EXECUTED),
    /**
     * It includes all negative CommandResult
     */
    NEGATIVE(REFUSE, COMMAND_REFUSED, ARGS, ARGS_EXECUTED, REFUSE_EXCEPTION),
    /**
     * It includes all executed CommandResult
     */
    EXECUTED(ALLOW,REFUSE,ARGS_EXECUTED),
    /**
     * No signal
     */
    NONE(0);

    /**
     * Its internal value
     */
    private final int value;

    CommandResult(final CommandResult result, final CommandResult... results) {
        this(toInt(result, results));
    }

    CommandResult(final int value) {
        this.value = value;
    }

    private static int toInt(@NotNull final CommandResult result, @NotNull final CommandResult[] results) {
        int ret = result.getValue();
        for (final CommandResult r : results)
            ret |= r.getValue();
        return ret;
    }

    public int getValue() {
        return this.value;
    }

    public boolean contains(final CommandResult result) {
        return (this.value & result.getValue()) == result.getValue();
    }

    public boolean isExecuted() {
        return EXECUTED.contains(this);
    }
}
