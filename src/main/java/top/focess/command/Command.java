package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The base class for all command definitions.
 * <p>
 * A {@code Command} acts as a container for one or more execution paths (signatures). 
 * When a user inputs a command string, the framework routes it to the most specific 
 * {@link Executor} registered within this class.
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li><b>Construction:</b> The constructor sets basic metadata and calls {@link #init()}.</li>
 *   <li><b>Initialization:</b> Inside {@link #init()}, you must call 
 *       {@link #addExecutor(CommandExecutor, CommandArgument[])} to define what the command does.</li>
 *   <li><b>Routing:</b> The framework uses {@link #lookupKeys()} (name + aliases) to find 
 *       this command instance.</li>
 *   <li><b>Usage:</b> If argument parsing fails, {@link #usage(CommandSender)} is called 
 *       to provide feedback to the user.</li>
 * </ol>
 */
public abstract class Command {

    private static final int USAGE_LINES_PER_MESSAGE = 7;

    private final List<Executor> executors = Lists.newCopyOnWriteArrayList();
    private final String name;
    private final List<String> aliases;
    private final String description;
    
    @Nullable
    private CommandManager manager;
    private CommandPermission permission = CommandPermission.EVERYONE;
    private Predicate<CommandSender> executorPermission = i -> true;

    /**
     * Constructs a new Command instance.
     *
     * @param name        The primary name of the command.
     * @param description A brief description of what the command does.
     * @param aliases     Optional alternative names for the command.
     * @throws CommandLoadException if {@link #init()} throws an exception.
     */
    public Command(@NotNull final String name, @NotNull final String description, @NotNull final String... aliases) {
        this.name = name;
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.aliases = Lists.newArrayList(aliases);
        try {
            this.init();
        } catch (final Exception e) {
            throw new CommandLoadException(this.getClass(), e);
        }
    }

    /**
     * Unregisters all commands from the default manager.
     */
    public static void unregisterAll() {
        CommandManager.getDefault().unregisterAll();
    }

    /**
     * Retrieves all registered commands from the default manager.
     *
     * @return An unmodifiable list of commands.
     */
    @NotNull
    @UnmodifiableView
    public static List<Command> getCommands() {
        return CommandManager.getDefault().getCommands();
    }

    /**
     * Retrieves a command by name from the default manager.
     *
     * @param name The name or alias of the command.
     * @return The command instance, or null if not found.
     */
    @Nullable
    public static Command get(@NotNull final String name) {
        return CommandManager.getDefault().get(name);
    }

    /**
     * Registers a command to the default manager.
     *
     * @param command The command to register.
     */
    public static void register(@NotNull final Command command) {
        CommandManager.getDefault().register(command);
    }

    /**
     * Checks if this command is currently registered to a manager.
     *
     * @return true if registered, false otherwise.
     */
    public boolean isRegistered() {
        return this.manager != null;
    }

    /**
     * Unregisters this command from its manager and clears its executors.
     */
    public void unregister() {
        final CommandManager current = this.manager;
        this.executors.clear();
        if (current != null)
            current.unregister(this);
    }

    void setManager(@NotNull final CommandManager manager) {
        this.manager = manager;
    }

    void clearManager() {
        this.manager = null;
    }

    @NotNull
    List<String> lookupKeys() {
        final List<String> keys = Lists.newArrayList(this.name.toLowerCase());
        for (final String alias : this.aliases)
            keys.add(alias.toLowerCase());
        return keys;
    }

    /**
     * Gets the primary name of this command.
     *
     * @return The command name.
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Gets the aliases for this command.
     *
     * @return A list of aliases.
     */
    @NotNull
    public List<String> getAliases() {
        return this.aliases;
    }

    /**
     * Gets the description of this command.
     *
     * @return The description string.
     */
    @NotNull
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the dynamic permission predicate for this command's executors.
     *
     * @return The permission predicate.
     */
    public Predicate<CommandSender> getExecutorPermission() {
        return this.executorPermission;
    }

    /**
     * Sets a dynamic permission predicate that applies to all executors of this command.
     *
     * @param executorPermission The new permission predicate.
     */
    public void setExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
        this.executorPermission = executorPermission;
    }

    /**
     * Gets the required permission level for this command.
     *
     * @return The command permission.
     */
    @NotNull
    public CommandPermission getPermission() {
        return this.permission;
    }

    /**
     * Sets the required permission level for this command.
     *
     * @param permission The new permission level.
     */
    public void setPermission(final CommandPermission permission) {
        this.permission = permission;
    }

    /**
     * Adds a new execution path to this command.
     *
     * @param executor         The logic to run when this signature is matched.
     * @param commandArguments The sequence of arguments that define this signature.
     * @return The created {@link Executor} instance for further configuration.
     */
    @NotNull
    public final Executor addExecutor(@NotNull final CommandExecutor executor, @NotNull final CommandArgument<?>... commandArguments) {
        final Executor executor1 = new Executor(executor, this, commandArguments);
        this.executors.add(executor1);
        return executor1;
    }

    /**
     * Initializes the command by registering its executors.
     * <p>
     * This method is called automatically during command construction. Implementations 
     * should use {@link #addExecutor(CommandExecutor, CommandArgument[])} to define 
     * the command's behavior and arguments.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * @Override
     * public void init() {
     *     this.addExecutor((s, data) -> {
     *         s.output("Hello " + data.get());
     *         return CommandResult.ALLOW;
     *     }, CommandArgument.ofString().named("name"));
     * }
     * }
     * </pre>
     */
    public abstract void init();

    /**
     * Provides the usage help lines for this command.
     * <p>
     * This method is called when the user provides invalid arguments or explicitly 
     * requests help. The returned lines will be sent to the {@link CommandSender}.
     *
     * @param sender The entity requesting help. Can be used to tailor the help 
     *               message based on permissions.
     * @return A non-null list of strings, each representing a line of help text.
     */
    @NotNull
    public abstract List<String> usage(CommandSender sender);

    @NotNull
    List<Executor> getExecutors() {
        return this.executors;
    }

    void infoUsage(@NotNull final CommandSender sender) {
        final List<String> usage = this.usage(sender);
        for (int start = 0; start < usage.size(); start += USAGE_LINES_PER_MESSAGE) {
            final int end = Math.min(start + USAGE_LINES_PER_MESSAGE, usage.size());
            sender.output(String.join("\n", usage.subList(start, end)));
        }
    }

    /**
     * Define a specific executor for this command with its own arguments and permissions.
     */
    public static class Executor {
        private final Map<CommandResult, CommandResultExecutor> results = new HashMap<>();
        private final CommandExecutor executor;
        private final CommandArgument<?>[] commandArguments;
        private final Command command;
        private final int nullableCommandArguments;
        private CommandPermission permission;
        private Predicate<CommandSender> executorPermission;

        private Executor(final CommandExecutor executor, final Command command, final CommandArgument<?>[] commandArguments) {
            this.executor = executor;
            this.command = command;
            this.commandArguments = commandArguments;
            this.nullableCommandArguments = (int) Arrays.stream(commandArguments).filter(CommandArgument::isNullable).count();
            this.permission = command.getPermission();
            this.executorPermission = command.getExecutorPermission();
        }

        /**
         * Sets the required permission level for this specific executor.
         *
         * @param permission The new permission level.
         * @return This executor, for chaining.
         */
        @NotNull
        public Executor setPermission(@NotNull final CommandPermission permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Attaches a callback to be run when this executor finishes with a specific result.
         *
         * @param result   The result state to track.
         * @param executor The callback logic.
         * @return This executor, for chaining.
         */
        @NotNull
        public Executor addCommandResultExecutor(@NotNull final CommandResult result, @NotNull final CommandResultExecutor executor) {
            this.results.put(result, executor);
            return this;
        }

        /**
         * Adds an additional dynamic permission check to this executor.
         *
         * @param executorPermission The dynamic predicate to add.
         * @return This executor, for chaining.
         */
        @NotNull
        public Executor addExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = this.executorPermission.and(executorPermission);
            return this;
        }

        /**
         * Removes all dynamic permission checks from this executor.
         *
         * @return This executor, for chaining.
         */
        @NotNull
        public Executor removeExecutorPermission() {
            this.executorPermission = i -> true;
            return this;
        }

        /**
         * Overwrites all dynamic permission checks with a new one.
         *
         * @param executorPermission The new dynamic predicate.
         * @return This executor, for chaining.
         */
        @NotNull
        public Executor overrideExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = executorPermission;
            return this;
        }

        /**
         * Gets the parent command this executor belongs to.
         *
         * @return The parent command.
         */
        public Command getCommand() {
            return this.command;
        }

        /**
         * Gets the execution logic for this signature.
         *
         * @return The command executor.
         */
        @NotNull
        CommandExecutor getExecutor() { return executor; }

        /**
         * Gets the signature of arguments expected by this executor.
         *
         * @return An array of command arguments.
         */
        @NotNull
        public CommandArgument<?>[] getCommandArguments() { return commandArguments; }

        /**
         * Gets the permission level required for this executor.
         *
         * @return The command permission.
         */
        @NotNull
        CommandPermission getPermission() { return permission; }

        /**
         * Gets the dynamic permission predicate for this executor.
         *
         * @return The permission predicate.
         */
        @NotNull
        Predicate<CommandSender> getExecutorPermission() { return executorPermission; }

        /**
         * Gets the count of nullable (optional) arguments in this signature.
         *
         * @return The nullable argument count.
         */
        int getNullableCommandArguments() { return nullableCommandArguments; }

        /**
         * Gets all registered result callbacks for this executor.
         *
         * @return A map of results to their corresponding executors.
         */
        @NotNull
        Map<CommandResult, CommandResultExecutor> getResults() { return results; }
    }
}
