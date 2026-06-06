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
 * Represent a Command that can be registered and executed.
 * <p>
 * Commands are data structures that define name, description, and a set of executors.
 * Permission gating is handled at the {@link Executor} level.
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

    public static void unregisterAll() {
        CommandManager.getDefault().unregisterAll();
    }

    @NotNull
    @UnmodifiableView
    public static List<Command> getCommands() {
        return CommandManager.getDefault().getCommands();
    }

    @Nullable
    public static Command get(@NotNull final String name) {
        return CommandManager.getDefault().get(name);
    }

    public static void register(@NotNull final Command command) {
        CommandManager.getDefault().register(command);
    }

    public boolean isRegistered() {
        return this.manager != null;
    }

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

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getAliases() {
        return this.aliases;
    }

    @NotNull
    public String getDescription() {
        return this.description;
    }

    public Predicate<CommandSender> getExecutorPermission() {
        return this.executorPermission;
    }

    public void setExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
        this.executorPermission = executorPermission;
    }

    @NotNull
    public CommandPermission getPermission() {
        return this.permission;
    }

    public void setPermission(final CommandPermission permission) {
        this.permission = permission;
    }

    @NotNull
    public final Executor addExecutor(@NotNull final CommandExecutor executor, @NotNull final CommandArgument<?>... commandArguments) {
        final Executor executor1 = new Executor(executor, this, commandArguments);
        this.executors.add(executor1);
        return executor1;
    }

    public abstract void init();

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

        @NotNull
        public Executor setPermission(@NotNull final CommandPermission permission) {
            this.permission = permission;
            return this;
        }

        @NotNull
        public Executor addCommandResultExecutor(@NotNull final CommandResult result, @NotNull final CommandResultExecutor executor) {
            this.results.put(result, executor);
            return this;
        }

        @NotNull
        public Executor addExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = this.executorPermission.and(executorPermission);
            return this;
        }

        @NotNull
        public Executor removeExecutorPermission() {
            this.executorPermission = i -> true;
            return this;
        }

        @NotNull
        public Executor overrideExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = executorPermission;
            return this;
        }

        public Command getCommand() {
            return this.command;
        }

        @NotNull
        CommandExecutor getExecutor() { return executor; }
        @NotNull
        public CommandArgument<?>[] getCommandArguments() { return commandArguments; }
        @NotNull
        CommandPermission getPermission() { return permission; }
        @NotNull
        Predicate<CommandSender> getExecutorPermission() { return executorPermission; }
        int getNullableCommandArguments() { return nullableCommandArguments; }
        @NotNull
        Map<CommandResult, CommandResultExecutor> getResults() { return results; }
    }
}
