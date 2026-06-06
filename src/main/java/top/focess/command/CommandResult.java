package top.focess.command;

import org.jetbrains.annotations.NotNull;

/**
 * The state and result of a command routing or execution process.
 * <p>
 * This enum defines the possible outcomes. Some states are generated internally
 * by the routing framework, while others are explicitly returned by the user
 * from their {@link CommandExecutor}.
 */
public enum CommandResult {

    /**
     * [USER STATE]
     * Returned explicitly by a user's {@link CommandExecutor} to indicate that
     * the command logic executed successfully and completed its intended action.
     */
    ALLOW(1),

    /**
     * [USER STATE]
     * Returned explicitly by a user's {@link CommandExecutor} to indicate that
     * the command logic was executed, but it deliberately refused to perform
     * the action (e.g., due to game rules, business logic, or invalid state).
     */
    REFUSE(2),

    /**
     * [FRAMEWORK STATE]
     * Returned by the routing framework when the command exists and the sender has
     * permission, but the provided arguments do not match any defined executor.
     * This state triggers the framework to print the command's usage help to the sender.
     */
    ARGS_NOT_EXECUTED(8),

    /**
     * [USER STATE]
     * Returned explicitly by a user's {@link CommandExecutor} to indicate that
     * execution proceeded, but the framework should still print the usage help
     * to the sender (e.g., for commands that have optional sub-menus).
     */
    ARGS(16),

    /**
     * [FRAMEWORK STATE]
     * Returned by the routing framework when an unexpected {@link Exception} is
     * thrown during the execution of a user's {@link CommandExecutor}.
     */
    REFUSE_EXCEPTION(32),

    /**
     * [FRAMEWORK STATE]
     * Returned by the routing framework when the input string specifies a top-level
     * command name that has not been registered in the {@link CommandManager}.
     */
    COMMAND_NOT_FOUND(64),

    /**
     * [FRAMEWORK STATE]
     * An internal state used by {@link CommandRoute} to indicate that a valid
     * executor and argument match has been successfully found, and the route
     * is ready for execution. This is never returned by the user.
     */
    MATCHED(128),

    /**
     * It includes all CommandResult values.
     */
    ALL(ALLOW, REFUSE, ARGS_NOT_EXECUTED, ARGS, REFUSE_EXCEPTION, COMMAND_NOT_FOUND, MATCHED),

    /**
     * It includes all negative CommandResult values (failures, refusals, exceptions).
     */
    NEGATIVE(REFUSE, ARGS_NOT_EXECUTED, ARGS, REFUSE_EXCEPTION, COMMAND_NOT_FOUND),

    /**
     * It includes all states indicating that a user's CommandExecutor was actually invoked.
     */
    EXECUTED(ALLOW, REFUSE, ARGS),

    /**
     * It includes all states that can be legally tracked by a {@link CommandResultExecutor}.
     * States like COMMAND_NOT_FOUND cannot be tracked because no specific executor is matched.
     */
    TRACKABLE(ALLOW, REFUSE, ARGS, REFUSE_EXCEPTION),

    /**
     * [FRAMEWORK STATE]
     * An empty or default state indicating no resolution has occurred, or a command
     * is completely invisible to the sender.
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
        return this != NONE && EXECUTED.contains(this);
    }

    public boolean isTrackable() {
        return this != NONE && TRACKABLE.contains(this);
    }
}
