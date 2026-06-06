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
     * Indicates that the command logic executed successfully and completed its intended action.
     * <p>
     * <b>Handling:</b> The {@link CommandExecutor} is responsible for providing any success 
     * messages to the sender before returning this state. The framework performs no additional 
     * actions.
     */
    ALLOW(1),

    /**
     * [USER STATE]
     * Indicates that the command logic was executed, but it deliberately refused to perform
     * the action (e.g., due to game rules, business logic, or invalid state).
     * <p>
     * <b>Handling:</b> The {@link CommandExecutor} is responsible for notifying the sender 
     * of the reason for refusal before returning this state. The framework performs no 
     * additional actions.
     */
    REFUSE(2),

    /**
     * [FRAMEWORK STATE]
     * Indicates that the command exists and the sender has permission, but the provided 
     * arguments do not match any defined executor.
     * <p>
     * <b>Handling:</b> Automatically handled by the framework, which prints the command's 
     * usage help to the {@link CommandSender}.
     */
    ARGS_NOT_EXECUTED(4),

    /**
     * [USER STATE]
     * Indicates that execution proceeded, but the caller should still be notified of usage 
     * requirements (e.g., for commands that have optional sub-menus or partially valid args).
     * <p>
     * <b>Handling:</b> Automatically handled by the framework, which prints the command's 
     * usage help to the {@link CommandSender}.
     */
    ARGS(8),

    /**
     * [FRAMEWORK STATE]
     * Indicates that an unexpected {@link Exception} was thrown during the execution of 
     * a user's {@link CommandExecutor}.
     * <p>
     * <b>Handling:</b> <b>Explicitly Handled.</b> The framework catches the exception and 
     * returns this state. The caller of {@link CommandManager#dispatch(CommandSender, String)} 
     * should handle this by logging the error or notifying the sender of an internal failure.
     */
    REFUSE_EXCEPTION(16),

    /**
     * [FRAMEWORK STATE]
     * Indicates that the input string specifies a top-level command name that has not 
     * been registered or is invisible to the sender.
     * <p>
     * <b>Handling:</b> <b>Explicitly Handled.</b> The framework returns this state immediately. 
     * The caller should handle this by notifying the sender that the command was not found.
     */
    COMMAND_NOT_FOUND(32),

    /**
     * [FRAMEWORK STATE]
     * An internal state used by {@link CommandRoute} to indicate that a valid
     * executor and argument match has been successfully found.
     */
    MATCHED(64),

    /**
     * Includes all CommandResult values.
     */
    ALL(ALLOW, REFUSE, ARGS_NOT_EXECUTED, ARGS, REFUSE_EXCEPTION, COMMAND_NOT_FOUND, MATCHED),

    /**
     * Includes all negative CommandResult values (failures, refusals, exceptions, not found).
     */
    NEGATIVE(REFUSE, ARGS_NOT_EXECUTED, ARGS, REFUSE_EXCEPTION, COMMAND_NOT_FOUND),

    /**
     * Includes all states indicating that a user's {@link CommandExecutor} was actually invoked.
     */
    EXECUTED(ALLOW, REFUSE, ARGS),

    /**
     * Includes all states that can be legally tracked by a {@link CommandResultExecutor}.
     * {@link #COMMAND_NOT_FOUND} cannot be tracked because no specific executor is matched.
     */
    TRACKABLE(ALLOW, REFUSE, ARGS, REFUSE_EXCEPTION),

    /**
     * Includes all states that need the user (caller of the dispatch method) to handle 
     * explicitly, as they are not handled by the internal framework or the executor.
     */
    EXPLICIT(REFUSE_EXCEPTION, COMMAND_NOT_FOUND),

    /**
     * [FRAMEWORK STATE]
     * An empty or default state indicating no resolution has occurred.
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

    public boolean isExplicit() {
        return this != NONE && EXPLICIT.contains(this);
    }
}
