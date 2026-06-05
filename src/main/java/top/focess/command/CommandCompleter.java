package top.focess.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A functional interface to provide auto-complete suggestions.
 */
@FunctionalInterface
public interface CommandCompleter {
    /**
     * Get the auto-complete suggestions for this argument.
     *
     * @param sender  the executor
     * @param command the command
     * @param args    all the arguments that command spilt by spaces, the last one is the partial argument
     * @return the auto-complete suggestions
     */
    @NotNull
    List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String[] args);
}
