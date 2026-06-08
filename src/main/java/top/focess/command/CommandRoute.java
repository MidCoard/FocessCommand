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

    /**
     * Constructs a new CommandRoute instance.
     *
     * @param manager The manager instance.
     * @param sender  The sender initiating the command.
     * @param input   The raw input string.
     * @param tokens  The tokenized input.
     */
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

        // 2. Resolve arguments using BFS
        boolean anyVisible = false;
        List<SearchState> currentStates = Lists.newArrayList();
        for (Command.Executor executor : this.command.getExecutors()) {
            if (sender.hasPermission(executor.getPermission()) && executor.getExecutorPermission().test(sender)) {
                anyVisible = true;
                currentStates.add(new SearchState(executor, 0, new String[executor.getCommandArguments().length]));
            }
        }

        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i).content();
            List<SearchState> nextStates = Lists.newArrayList();
            boolean isLastToken = (i == tokens.size() - 1);

            for (SearchState state : currentStates) {
                CommandArgument<?>[] cmdArgs = state.executor.getCommandArguments();
                for (int j = state.argIndex; j < cmdArgs.length; j++) {
                    // Collect completions for the last token position
                    if (isLastToken) {
                        this.completions.addAll(cmdArgs[j].complete(sender, this.command, tokens.stream().map(Token::content).toArray(String[]::new)));
                        this.currentArguments.add(cmdArgs[j]);
                    }

                    // Try to match the current token to this argument
                    if (cmdArgs[j].accept(token)) {
                        nextStates.add(state.next(j, token));
                    }

                    // If this argument is NOT nullable, we cannot skip it to match the current token to a later argument
                    if (!cmdArgs[j].isNullable())
                        break;
                }
            }
            currentStates = nextStates;
            if (currentStates.isEmpty() && !isLastToken) break;
        }

        // 3. Identify the best match from terminal states
        for (SearchState state : currentStates) {
            if (state.isTerminal()) {
                this.matchedExecutor = state.executor;
                this.matchedData = state.buildData();
                this.state = CommandResult.MATCHED;
                break; // Take the first valid match (greedy + registration order)
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

    private record SearchState(Command.Executor executor, int argIndex, String[] values) {

        public SearchState next(int j, String token) {
            String[] nextValues = Arrays.copyOf(this.values, this.values.length);
            nextValues[j] = token;
            return new SearchState(this.executor, j + 1, nextValues);
        }

        public boolean isTerminal() {
            CommandArgument<?>[] cmdArgs = executor.getCommandArguments();
            for (int i = argIndex; i < cmdArgs.length; i++)
                if (!cmdArgs[i].isNullable())
                    return false;
            return true;
        }

        public DataCollection buildData() {
            CommandArgument<?>[] cmdArgs = executor.getCommandArguments();
            DataCollection data = new DataCollection(Arrays.stream(cmdArgs).map(CommandArgument::getDataConverter).toArray(DataConverter[]::new));
            for (int i = 0; i < cmdArgs.length; i++) {
                if (values[i] != null) {
                    cmdArgs[i].put(data, values[i]);
                }
            }
            data.flip();
            return data;
        }
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

    /**
     * Gets the sender associated with this route.
     *
     * @return The command sender.
     */
    @NotNull
    public CommandSender getSender() { return sender; }

    /**
     * Gets the raw input string that was routed.
     *
     * @return The raw input.
     */
    @NotNull
    public String getInput() { return input; }

    /**
     * Gets the tokenized version of the input string.
     *
     * @return A list of token contents.
     */
    @NotNull
    public List<String> getTokens() { return tokens.stream().map(Token::content).collect(Collectors.toList()); }

    /**
     * Gets the root command matched by the routing engine.
     *
     * @return The matched command, or null if no command was found.
     */
    @Nullable
    public Command getCommand() { return command; }

    /**
     * Gets the specific executor matched by the routing engine.
     *
     * @return The matched executor, or null if no signature matched or 
     *         permissions were insufficient.
     */
    @Nullable
    public Command.Executor getExecutor() { return matchedExecutor; }

    /**
     * Gets the parsed argument data for the matched executor.
     *
     * @return The data collection, or null if no executor was matched.
     */
    @Nullable
    public DataCollection getData() { return matchedData; }

    /**
     * Gets the current routing state.
     *
     * @return The command result state.
     */
    @NotNull
    public CommandResult getState() { return state; }
}
