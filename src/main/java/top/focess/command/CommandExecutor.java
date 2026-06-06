package top.focess.command;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the core execution logic for a specific command signature.
 * <p>
 * A {@code CommandExecutor} is responsible for interpreting parsed arguments and performing
 * the intended action. It is typically registered to a {@link Command} using 
 * {@link Command#addExecutor(CommandExecutor, CommandArgument[])}.
 * 
 * <h2>Implementation Requirements</h2>
 * <ul>
 *   <li><b>Statelessness:</b> Executors should ideally be stateless, as they may be invoked 
 *       concurrently or reused across different command dispatches.</li>
 *   <li><b>Return Codes:</b> The implementation MUST return exactly one of the three 
 *       "User States":
 *       <ul>
 *         <li>{@link CommandResult#ALLOW}: The command was successful.</li>
 *         <li>{@link CommandResult#REFUSE}: The command was valid but logically refused.</li>
 *         <li>{@link CommandResult#ARGS}: The command should be treated as a mismatch, triggering usage help.</li>
 *       </ul>
 *   </li>
 *   <li><b>Exceptions:</b> While the framework catches {@link Exception} and returns 
 *       {@link CommandResult#REFUSE_EXCEPTION}, executors should ideally handle their 
 *       own business-logic errors and return {@link CommandResult#REFUSE}.</li>
 * </ul>
 */
@FunctionalInterface
public interface CommandExecutor {
    /**
     * Executes the command logic.
     *
     * @param sender         The entity that initiated the command. Provides I/O capabilities 
     *                       (output messages, interactive input) and carries permissions.
     * @param dataCollection A type-safe container holding all arguments parsed for this 
     *                       specific execution path. Arguments can be retrieved by type, 
     *                       position, or name.
     * @return A {@link CommandResult} indicating the outcome. MUST be ALLOW, REFUSE, or ARGS.
     */
    @NotNull
    CommandResult execute(@NotNull CommandSender sender, @NotNull DataCollection dataCollection);
}
