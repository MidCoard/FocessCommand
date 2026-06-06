package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides logic for generating tab-completion suggestions for command arguments.
 * <p>
 * A {@code CommandCompleter} can be attached to a {@link CommandArgument} to override 
 * or enhance the default suggestions provided by the {@link DataConverter}.
 */
@FunctionalInterface
public interface CommandCompleter {
    /**
     * Generates a list of completion suggestions.
     *
     * @param sender  The entity requesting completions. Can be used for permission-based 
     *                suggestions.
     * @param command The root command being completed.
     * @param args    The current array of input tokens. The last element ({@code args[args.length - 1]}) 
     *                is the partial string currently being typed by the user.
     * @return A non-null list of {@link CommandCompletion} objects. Return an empty list 
     *         if no suggestions are available.
     */
    @NotNull
    List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String[] args);
}
