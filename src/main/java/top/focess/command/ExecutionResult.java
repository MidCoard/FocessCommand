package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A richer result of executing a {@link Command}, pairing the {@link CommandResult} status with an
 * optional human-readable message (for example, the message of the exception that caused a
 * {@link CommandResult#REFUSE_EXCEPTION}).
 * <p>
 * This complements the compact {@link CommandResult} bitmask by letting callers receive additional
 * feedback without having to catch exceptions themselves.
 *
 * @see CommandManager#dispatch(CommandSender, String)
 */
public record ExecutionResult(CommandResult result, @Nullable String message) {

    public ExecutionResult(@NotNull final CommandResult result, @Nullable final String message) {
        this.result = Objects.requireNonNull(result, "result");
        this.message = message;
    }

    /**
     * Create an {@code ExecutionResult} without a message.
     *
     * @param result the command result status
     * @return the execution result
     */
    @NotNull
    public static ExecutionResult of(@NotNull final CommandResult result) {
        return new ExecutionResult(result, null);
    }

    /**
     * Create an {@code ExecutionResult} with a message.
     *
     * @param result  the command result status
     * @param message the additional feedback message, may be null
     * @return the execution result
     */
    @NotNull
    public static ExecutionResult of(@NotNull final CommandResult result, @Nullable final String message) {
        return new ExecutionResult(result, message);
    }

    /**
     * Get the command result status.
     *
     * @return the command result status
     */
    @Override
    @NotNull
    public CommandResult result() {
        return this.result;
    }

    /**
     * Get the additional feedback message, if any.
     *
     * @return the message wrapped in an {@link Optional}
     */
    @NotNull
    public Optional<String> getMessage() {
        return Optional.ofNullable(this.message);
    }

    /**
     * Indicate whether the command was actually executed.
     *
     * @return true if the underlying {@link CommandResult} is an executed result
     * @see CommandResult#isExecuted()
     */
    public boolean isExecuted() {
        return this.result.isExecuted();
    }
}
