package top.focess.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A data carrier for a single tab-completion suggestion.
 *
 * @param candidate   The string to be inserted into the command line if selected.
 * @param description An optional human-readable description of the suggestion 
 *                    (e.g., "The target player's name").
 */
public record CommandCompletion(@NotNull String candidate, @Nullable String description) {

    /**
     * Canonical constructor for {@code CommandCompletion}.
     * 
     * @param candidate   The suggestion string.
     * @param description The optional description.
     */
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
