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
 * An instance-based registry of {@link Command}s.
 * <p>
 * This class handles the registration of commands and provides the entry point
 * for routing, dispatching, and completion.
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
        return new CommandRoute(sender, input, tokenize(input)).resolve(this);
    }

    public static class Token {
        public final String content;
        public final boolean isQuoted;
        public final boolean isUnclosed;

        Token(String content, boolean isQuoted, boolean isUnclosed) {
            this.content = content;
            this.isQuoted = isQuoted;
            this.isUnclosed = isUnclosed;
        }
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
