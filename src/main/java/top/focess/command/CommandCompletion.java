package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an auto-complete suggestion with an optional description.
 *
 * @param candidate   the candidate string
 * @param description the description of the candidate
 */
public record CommandCompletion(@NotNull String candidate, @Nullable String description) {

    public CommandCompletion {
        Objects.requireNonNull(candidate, "candidate cannot be null");
    }

    /**
     * Create a new CommandCompletion with a candidate and no description.
     *
     * @param candidate the candidate string
     * @return a new CommandCompletion
     */
    @NotNull
    public static CommandCompletion of(@NotNull final String candidate) {
        return new CommandCompletion(candidate, null);
    }

    /**
     * Create a new CommandCompletion with a candidate and a description.
     *
     * @param candidate   the candidate string
     * @param description the description
     * @return a new CommandCompletion
     */
    @NotNull
    public static CommandCompletion of(@NotNull final String candidate, @Nullable final String description) {
        return new CommandCompletion(candidate, description);
    }
}
