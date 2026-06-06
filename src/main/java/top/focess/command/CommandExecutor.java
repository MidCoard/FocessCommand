package top.focess.command;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a command executor to define how to execute command.
 * <p>
 * This is a functional interface whose functional method is {@link CommandExecutor#execute(CommandSender, DataCollection)}.
 */
@FunctionalInterface
public interface CommandExecutor {
    /**
     * Used to execute the command under certain conditions.
     *
     * @param sender         the sender of the command, who also handles I/O
     * @param dataCollection the parsed arguments the command received
     * @return the result of this execution
     */
    @NotNull
    CommandResult execute(@NotNull CommandSender sender, @NotNull DataCollection dataCollection);
}
