package top.focess.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An instance-based registry of {@link Command}s.
 * <p>
 * Unlike the static convenience methods on {@link Command} (which delegate to a single shared
 * {@link #getDefault() default} manager), each {@code CommandManager} owns an isolated command
 * namespace. This makes it possible to scope commands per host/plugin and to test registration
 * behavior without touching global state.
 */
public class CommandManager {

    private static final CommandManager DEFAULT = new CommandManager();

    /**
     * Lookup map keyed by lower-cased command name and aliases.
     */
    private final Map<String, Command> commandsMap = Maps.newConcurrentMap();

    /**
     * All currently registered commands (each command appears once).
     */
    private final List<Command> commands = Lists.newCopyOnWriteArrayList();

    /**
     * Get the shared default command manager backing the static {@link Command} methods.
     *
     * @return the default command manager
     */
    @NotNull
    public static CommandManager getDefault() {
        return DEFAULT;
    }

    /**
     * Register the command in this manager.
     *
     * @param command the command that need to be registered
     * @throws CommandDuplicateException if the command name or any alias already exists in this manager
     * @throws IllegalStateException     if the command is not initialized
     */
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

    /**
     * Unregister the command from this manager.
     *
     * @param command the command that need to be unregistered
     */
    public void unregister(@NotNull final Command command) {
        for (final String key : command.lookupKeys())
            this.commandsMap.remove(key, command);
        this.commands.remove(command);
        command.clearManager();
    }

    /**
     * Unregister all commands registered in this manager.
     */
    public void unregisterAll() {
        for (final Command command : Lists.newArrayList(this.commands))
            command.unregister();
    }

    /**
     * Get a registered command by its name or one of its aliases (case-insensitive).
     *
     * @param name the name or alias of the command
     * @return the matching command, or null if none is registered under that key
     */
    @Nullable
    public Command get(@NotNull final String name) {
        return this.commandsMap.get(name.toLowerCase());
    }

    /**
     * Get all commands registered in this manager.
     *
     * @return all commands as an unmodifiable list
     */
    @NotNull
    @UnmodifiableView
    public List<Command> getCommands() {
        return Collections.unmodifiableList(Lists.newArrayList(this.commands));
    }

    /**
     * Get the auto-complete suggestions for the given input.
     *
     * @param sender    the executor
     * @param input     the raw input string
     * @return the auto-complete suggestions
     */
    @NotNull
    public List<String> complete(@NotNull final CommandSender sender, @NotNull final String input) {
        final List<String> split = split(input, true);
        if (split.isEmpty())
            return List.of();
        if (split.size() <= 1) {
            final String name = split.get(0).toLowerCase();
            return this.commandsMap.keySet().stream()
                    .filter(key -> key.startsWith(name))
                    .filter(key -> {
                        final Command command = this.commandsMap.get(key);
                        return command != null && sender.hasPermission(command.getPermission());
                    })
                    .collect(Collectors.toList());
        }
        final Command command = this.get(split.get(0));
        if (command == null)
            return List.of();
        final String[] args = new String[split.size() - 1];
        for (int i = 1; i < split.size(); i++)
            args[i - 1] = split.get(i);
        return command.complete(sender, args);
    }

    /**
     * Execute a command from a raw input string.
     *
     * @param sender    the executor
     * @param input     the raw input string (e.g., "tp player 10 20 30")
     * @param ioHandler the receiver
     * @return the execution result
     */
    @NotNull
    public ExecutionResult dispatch(@NotNull final CommandSender sender, @NotNull final String input, @NotNull final IOHandler ioHandler) {
        final List<String> split = split(input, false);
        if (split.isEmpty())
            return ExecutionResult.of(CommandResult.NONE);
        final Command command = this.get(split.get(0));
        if (command == null)
            return ExecutionResult.of(CommandResult.COMMAND_REFUSED);
        final String[] args = new String[split.size() - 1];
        for (int i = 1; i < split.size(); i++)
            args[i - 1] = split.get(i);
        return command.execute(sender, args, ioHandler);
    }

    @NotNull
    private List<String> split(@NotNull String input, boolean includeTrailingEmpty) {
        final List<String> result = Lists.newArrayList();
        final StringBuilder current = new StringBuilder();
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean hasArg = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                hasArg = true;
            } else if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                hasArg = true;
            } else if (Character.isWhitespace(c) && !inDoubleQuote && !inSingleQuote) {
                if (hasArg || !current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                    hasArg = false;
                }
            } else {
                current.append(c);
                hasArg = true;
            }
        }
        if (hasArg || !current.isEmpty())
            result.add(current.toString());
        if (includeTrailingEmpty && (input.isEmpty() || (Character.isWhitespace(input.charAt(input.length() - 1)) && !inDoubleQuote && !inSingleQuote)))
            result.add("");
        return result;
    }
}
