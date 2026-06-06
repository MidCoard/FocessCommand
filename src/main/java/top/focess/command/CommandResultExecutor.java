package top.focess.command;

/**
 * Defines a callback to be executed after a {@link CommandExecutor} has finished.
 * <p>
 * This interface allows developers to attach post-execution logic (e.g., logging, 
 * auditing, or triggering side effects) based on the specific {@link CommandResult} 
 * returned by the command.
 * 
 * <h2>Usage</h2>
 * Use {@link Command.Executor#addCommandResultExecutor(CommandResult, CommandResultExecutor)} 
 * to subscribe to specific outcomes.
 */
@FunctionalInterface
public interface CommandResultExecutor {

    /**
     * Responds to a command completion event.
     *
     * @param commandResult The specific result returned by the framework or the executor. 
     *                      Commonly used to distinguish between success (ALLOW) and 
     *                      various refusal states.
     */
    void execute(CommandResult commandResult);
}
