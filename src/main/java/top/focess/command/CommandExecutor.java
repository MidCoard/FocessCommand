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
     * Used to execute the command under certain conditions
     *
     * @param sender         the executor of the command
     * @param dataCollection parse the arguments the command received
     * @return the result of this execution
     */
    @NotNull
    CommandResult execute(CommandSender sender, DataCollection dataCollection);

}
