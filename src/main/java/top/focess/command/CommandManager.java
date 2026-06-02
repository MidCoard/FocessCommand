package top.focess.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        if (command.getName() == null)
            throw new IllegalStateException("CommandType does not contain name or the constructor does not super name");
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
}
