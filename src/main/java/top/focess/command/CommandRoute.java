package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.command.CommandManager.Token;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The internal engine responsible for matching a raw command string to a specific executor.
 * <p>
 * {@code CommandRoute} unifies the logic for command dispatching and tab-completion. It 
 * performs a single, Depth-First Search (DFS) traversal of the command tree to resolve 
 * the following:
 * <ul>
 *   <li><b>Command Matching:</b> Identifying the root {@link Command} and the best 
 *       fitting {@link Command.Executor} signature.</li>
 *   <li><b>Argument Parsing:</b> Converting raw string tokens into typed values stored in 
 *       a {@link DataCollection}.</li>
 *   <li><b>Completion Discovery:</b> Finding all valid suggestions for the current 
 *       cursor position.</li>
 *   <li><b>Hint Discovery:</b> Identifying the expected {@link CommandArgument} types 
 *       at the current position.</li>
 * </ul>
 */
public class CommandRoute {

    private final CommandSender sender;
    private final String input;
    private final List<Token> tokens;
    
    private Command command;
    private Command.Executor matchedExecutor;
    private DataCollection matchedData;
    private final List<CommandCompletion> completions = Lists.newArrayList();
    private final List<CommandArgument<?>> currentArguments = Lists.newArrayList();
    private CommandResult state = CommandResult.NONE;

    public CommandRoute(@NotNull CommandManager manager, @NotNull CommandSender sender, @NotNull String input, @NotNull List<Token> tokens) {
        this.sender = sender;
        this.input = input;
        this.tokens = tokens;
        this.resolve(manager);
    }

    private void resolve(@NotNull CommandManager manager) {
        if (tokens.isEmpty()) {
            this.state = CommandResult.COMMAND_NOT_FOUND;
            return;
        }

        String commandName = tokens.get(0).content();
        this.command = manager.get(commandName);

        // 1. Resolve top-level command completions if it's a single token
        if (tokens.size() == 1) {
            String partial = commandName.toLowerCase();
            this.completions.addAll(manager.getCommands().stream()
                    .filter(c -> c.getName().toLowerCase().startsWith(partial) || c.getAliases().stream().anyMatch(a -> a.toLowerCase().startsWith(partial)))
                    .filter(c -> sender.hasPermission(c.getPermission()) && c.getExecutorPermission().test(sender))
                    .filter(c -> c.getExecutors().stream().anyMatch(e -> sender.hasPermission(e.getPermission()) && e.getExecutorPermission().test(sender)))
                    .map(c -> CommandCompletion.of(c.getName(), c.getDescription()))
                    .toList());
        }

        if (this.command == null) {
            this.state = CommandResult.COMMAND_NOT_FOUND;
            return;
        }

        // 2. Resolve arguments (Executors and Completions)
        String[] args = new String[tokens.size() - 1];
        for (int i = 1; i < tokens.size(); i++)
            args[i - 1] = tokens.get(i).content();

        boolean anyVisible = false;
        for (Command.Executor executor : this.command.getExecutors()) {
            if (sender.hasPermission(executor.getPermission()) && executor.getExecutorPermission().test(sender)) {
                anyVisible = true;
                // Collect completions for this executor
                this.resolveCompletions(executor, args);
                
                // Check if this executor matches for dispatch
                if (this.matchedExecutor == null) {
                    DataCollection data = this.checkMatch(executor, args);
                    if (data != null) {
                        this.matchedExecutor = executor;
                        this.matchedData = data;
                        this.state = CommandResult.MATCHED;
                    }
                }
            }
        }

        if (this.matchedExecutor == null) {
            this.state = anyVisible ? CommandResult.ARGS_NOT_EXECUTED : CommandResult.COMMAND_NOT_FOUND;
        }

        // Finalize completions: Handle trailing space/closed quote edge cases
        if (!tokens.isEmpty()) {
            Token lastToken = tokens.get(tokens.size() - 1);
            if (lastToken.isQuoted() && !lastToken.isUnclosed()) {
                if (!input.endsWith(" ") && !input.endsWith("\t")) {
                    this.completions.clear();
                    this.currentArguments.clear();
                }
            }
        }
    }

    private void resolveCompletions(Command.Executor executor, String[] args) {
        if (args.length == 0 || args.length > executor.getCommandArguments().length)
            return;
        this.dfsComplete(executor, args, 0, 0, executor.getCommandArguments().length - (args.length - 1));
    }

    private void dfsComplete(Command.Executor executor, String[] args, int indexOfArgs, int index, int nullableLeft) {
        CommandArgument<?>[] cmdArgs = executor.getCommandArguments();
        if (indexOfArgs == args.length - 1) {
            if (index < cmdArgs.length) {
                this.completions.addAll(cmdArgs[index].complete(sender, this.command, args));
                this.currentArguments.add(cmdArgs[index]);
            }
            if (index < cmdArgs.length && cmdArgs[index].isNullable() && nullableLeft > 0 && index + 1 < cmdArgs.length)
                this.dfsComplete(executor, args, indexOfArgs, index + 1, nullableLeft - 1);
            return;
        }
        if (index < cmdArgs.length && cmdArgs[index].isNullable() && nullableLeft > 0)
            this.dfsComplete(executor, args, indexOfArgs, index + 1, nullableLeft - 1);
        if (index < cmdArgs.length && cmdArgs[index].accept(args[indexOfArgs]))
            this.dfsComplete(executor, args, indexOfArgs + 1, index + 1, nullableLeft);
    }

    @Nullable
    private DataCollection checkMatch(Command.Executor executor, String[] args) {
        CommandArgument<?>[] cmdArgs = executor.getCommandArguments();
        if (args.length > cmdArgs.length)
            return null;
        if (args.length < cmdArgs.length - executor.getNullableCommandArguments())
            return null;
        
        List<CommandArgument<?>> matchedList = Lists.newArrayList();
        if (this.dfsMatch(executor, args, 0, 0, cmdArgs.length - args.length, matchedList)) {
            DataCollection data = new DataCollection(Arrays.stream(cmdArgs).map(CommandArgument::getDataConverter).toArray(DataConverter[]::new));
            for (int i = 0; i < args.length; i++)
                matchedList.get(i).put(data, args[i]);
            data.flip();
            return data;
        }
        return null;
    }

    private boolean dfsMatch(Command.Executor executor, String[] args, int indexOfArgs, int index, int nullableLeft, List<CommandArgument<?>> matchedList) {
        CommandArgument<?>[] cmdArgs = executor.getCommandArguments();
        if (indexOfArgs == args.length)
            return true;
        if (index >= cmdArgs.length)
            return false;
        if (cmdArgs[index].isNullable() && nullableLeft > 0) {
            if (this.dfsMatch(executor, args, indexOfArgs, index + 1, nullableLeft - 1, matchedList))
                return true;
        }
        if (cmdArgs[index].accept(args[indexOfArgs])) {
            matchedList.add(cmdArgs[index]);
            if (this.dfsMatch(executor, args, indexOfArgs + 1, index + 1, nullableLeft, matchedList))
                return true;
            matchedList.remove(matchedList.size() - 1);
        }
        return false;
    }

    /**
     * Executes the resolved command path.
     * <p>
     * This is the final step in the command lifecycle. It will:
     * <ol>
     *   <li>Check if a command and executor were successfully matched.</li>
     *   <li>Handle routing failures (e.g., printing usage on mismatch).</li>
     *   <li>Invoke the {@link CommandExecutor#execute(CommandSender, DataCollection)} logic.</li>
     *   <li>Trigger any registered {@link CommandResultExecutor} callbacks.</li>
     *   <li>Catch and wrap any unexpected exceptions into a {@link CommandResult#REFUSE_EXCEPTION}.</li>
     * </ol>
     *
     * @return An {@link ExecutionResult} containing the final status and optional error message.
     */
    @NotNull
    public ExecutionResult execute() {
        if (this.state == CommandResult.COMMAND_NOT_FOUND) {
            return ExecutionResult.of(CommandResult.COMMAND_NOT_FOUND);
        }

        if (this.state == CommandResult.ARGS_NOT_EXECUTED) {
            this.command.infoUsage(this.sender);
            return ExecutionResult.of(CommandResult.ARGS_NOT_EXECUTED);
        }

        if (this.state == CommandResult.MATCHED) {
            CommandResult result;
            String exceptionMessage = null;
            
            try {
                result = this.matchedExecutor.getExecutor().execute(this.sender, this.matchedData);
                
                if (result != CommandResult.ALLOW && result != CommandResult.REFUSE && result != CommandResult.ARGS) {
                    throw new IllegalStateException("CommandExecutor returned an invalid state: " + result.name() + ". Only ALLOW, REFUSE, or ARGS are acceptable.");
                }
            } catch (Exception e) {
                result = CommandResult.REFUSE_EXCEPTION;
                exceptionMessage = e.getMessage();
            }

            // Handle CommandResultExecutors for all trackable states (including exceptions)
            for (CommandResult r : this.matchedExecutor.getResults().keySet()) {
                if ((r.getValue() & result.getValue()) != 0) {
                    this.matchedExecutor.getResults().get(r).execute(result);
                }
            }

            if (result == CommandResult.ARGS) {
                this.command.infoUsage(this.sender);
            }
            
            return ExecutionResult.of(result, exceptionMessage);
        }

        throw new IllegalStateException("Unreachable state: " + this.state);
    }

    /**
     * Gets all valid tab-completion suggestions for the current cursor position.
     *
     * @return A non-null, distinct list of {@link CommandCompletion}s.
     */
    @NotNull
    public List<CommandCompletion> getCompletions() {
        return this.completions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Gets the expected {@link CommandArgument} objects at the current cursor position.
     * <p>
     * This is primarily used for displaying UI hints or placeholders (e.g., 
     * "&lt;name&gt; &mdash; The user to invite").
     *
     * @return A non-null, distinct list of possible arguments.
     */
    @NotNull
    public List<CommandArgument<?>> getCurrentArguments() {
        return this.currentArguments.stream().distinct().collect(Collectors.toList());
    }

    @NotNull
    public CommandSender getSender() { return sender; }
    @NotNull
    public String getInput() { return input; }
    @NotNull
    public List<String> getTokens() { return tokens.stream().map(Token::content).collect(Collectors.toList()); }
    @Nullable
    public Command getCommand() { return command; }
    @Nullable
    public Command.Executor getExecutor() { return matchedExecutor; }
    @Nullable
    public DataCollection getData() { return matchedData; }
    @NotNull
    public CommandResult getState() { return state; }
}
