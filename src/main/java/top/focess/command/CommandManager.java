package top.focess.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The central management and entry point for the Focess Command framework.
 * <p>
 * This class handles command registration, unregistration, auto-completion, and dispatching.
 * It coordinates the lifecycle of a command execution from raw string input to the invocation
 * of specific {@link CommandExecutor}s.
 * 
 * <h2>Framework Overview</h2>
 * The Focess Command framework operates on a **Route-Match-Execute** model:
 * <ol>
 *   <li><b>Routing:</b> The input is tokenized and traversed to find a matching {@link Command}.</li>
 *   <li><b>Matching:</b> The framework performs a Depth-First Search (DFS) across all registered
 *       executors for that command to find the best match based on argument types and constraints.</li>
 *   <li><b>Execution:</b> Once matched, the corresponding executor is invoked with a 
 *       {@link DataCollection} containing the parsed arguments.</li>
 * </ol>
 * 
 * <h2>State Handling</h2>
 * Every dispatch returns an {@link ExecutionResult} containing a {@link CommandResult}. 
 * Some states are handled internally (like printing usage on mismatch), while others 
 * ({@link CommandResult#isExplicit()}) must be handled by the caller.
 */
public class CommandManager {

    private static final CommandManager DEFAULT = new CommandManager();

    private final Map<String, Command> commandsMap = Maps.newHashMap();
    private final List<Command> commands = Lists.newArrayList();

    @NotNull
    public static CommandManager getDefault() {
        return DEFAULT;
    }

    public void register(@NotNull final Command command) {
        final List<String> keys = command.lookupKeys();
        for (final String key : keys)
            if (this.commandsMap.containsKey(key))
                throw new CommandDuplicateException(key);
        for (final String key : keys)
            this.commandsMap.put(key, command);
        this.commands.add(command);
        command.setManager(this);
    }

    public void unregister(@NotNull final Command command) {
        if (this.commands.remove(command)) {
            for (final String key : command.lookupKeys())
                this.commandsMap.remove(key);
            command.clearManager();
        }
    }

    public void unregisterAll() {
        for (final Command command : this.commands)
            command.clearManager();
        this.commands.clear();
        this.commandsMap.clear();
    }

    @Nullable
    public Command get(@NotNull final String name) {
        return this.commandsMap.get(name.toLowerCase());
    }

    @NotNull
    @UnmodifiableView
    public List<Command> getCommands() {
        return Collections.unmodifiableList(Lists.newArrayList(this.commands));
    }

    /**
     * Get auto-complete suggestions for the given input.
     */
    @NotNull
    public List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final String input) {
        return this.route(sender, input).getCompletions();
    }

    /**
     * Dispatch and execute a command from raw input.
     */
    @NotNull
    public ExecutionResult dispatch(@NotNull final CommandSender sender, @NotNull final String input) {
        return this.route(sender, input).execute();
    }

    /**
     * Resolves the input into a CommandRoute context.
     */
    @NotNull
    public CommandRoute route(@NotNull CommandSender sender, @NotNull String input) {
        return new CommandRoute(this, sender, input, tokenize(input));
    }

    record Token(String content, boolean isQuoted, boolean isUnclosed) {
    }

    @NotNull
    private List<Token> tokenize(@NotNull String input) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean hasArg = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\"' && !inSingleQuote) {
                if (inDoubleQuote) {
                    tokens.add(new Token(current.toString(), true, false));
                    current.setLength(0);
                    inDoubleQuote = false;
                    hasArg = false;
                } else {
                    inDoubleQuote = true;
                    hasArg = true;
                }
            } else if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote) {
                    tokens.add(new Token(current.toString(), true, false));
                    current.setLength(0);
                    inSingleQuote = false;
                    hasArg = false;
                } else {
                    inSingleQuote = true;
                    hasArg = true;
                }
            } else if (Character.isWhitespace(c) && !inDoubleQuote && !inSingleQuote) {
                if (hasArg || !current.isEmpty()) {
                    tokens.add(new Token(current.toString(), false, false));
                    current.setLength(0);
                    hasArg = false;
                }
            } else {
                current.append(c);
                hasArg = true;
            }
        }
        
        if (inDoubleQuote || inSingleQuote) {
            tokens.add(new Token(current.toString(), true, true));
        } else if (hasArg || !current.isEmpty()) {
            tokens.add(new Token(current.toString(), false, false));
        }

        if (input.isEmpty() || (Character.isWhitespace(input.charAt(input.length() - 1)) && !inDoubleQuote && !inSingleQuote)) {
            tokens.add(new Token("", false, false));
        }
        
        return tokens;
    }
}
