package top.focess.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Represent a Plugin class that can execute. Just like we use the terminal, we could use it to executing some commands. This is an important way to interact with FocessQQ Bot.
 * You should declare {@link CommandType} to this class ,or you should register it with your plugin manually.
 */
public abstract class Command {


    private static final Map<String, Command> COMMANDS_MAP = Maps.newConcurrentMap();

    private final List<Executor> executors = Lists.newCopyOnWriteArrayList();

    /**
     * The name of the command
     */
    private final String name;
    /**
     * The aliases of the command
     */
    private final List<String> aliases;

    /**
     * Indicate whether the command is registered or not
     */
    private boolean registered;

    /**
     * The MiraiPermission of the command
     */
    private CommandPermission permission;

    /**
     * The executor check predicate
     */
    private Predicate<CommandSender> executorPermission;

    /**
     * Instance a <code>Command</code> Class with special name and aliases.
     *
     * @param name    the name of the command
     * @param aliases the aliases of the command
     * @throws CommandLoadException if there is any exception thrown in the initializing process
     */
    public Command(@NotNull final String name, @NotNull final String... aliases) {
        this.name = name;
        this.aliases = Lists.newArrayList(aliases);
        this.permission = CommandPermission.MEMBER;
        this.executorPermission = i -> true;
        try {
            this.init();
        } catch (final Exception e) {
            throw new CommandLoadException(this.getClass(), e);
        }
    }

    /**
     * Unregister all commands
     */
    public static void unregisterAll() {
        for (final Command command : COMMANDS_MAP.values())
            command.unregister();
    }

    /**
     * Get all commands
     *
     * @return All commands as a list
     */
    @NotNull
    @UnmodifiableView
    public static List<Command> getCommands() {
        return Collections.unmodifiableList(Lists.newArrayList(COMMANDS_MAP.values()));
    }

    /**
     * Register the command
     *
     * @param command the command that need to be registered
     * @throws CommandDuplicateException if the command name already exists in the registered commands
     * @throws IllegalStateException    if the command is not initialized
     */
    public static void register(@NotNull final Command command) {
        if (command.name == null)
            throw new IllegalStateException("CommandType does not contain name or the constructor does not super name");
        List<String> commandNames = Lists.newArrayList(command.getName());
        commandNames.addAll(command.getAliases());
        for (final String commandName : commandNames)
            for (Map.Entry<String,Command> entry : COMMANDS_MAP.entrySet())
                if (entry.getKey().equalsIgnoreCase(commandName) || entry.getValue().getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(commandName)))
                    throw new CommandDuplicateException(commandName);
        command.registered = true;
        COMMANDS_MAP.put(command.getName(), command);
    }

    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * Unregister this command
     */
    public void unregister() {
        this.registered = false;
        this.executors.clear();
        COMMANDS_MAP.remove(this.getName());
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getAliases() {
        return this.aliases;
    }

    public Predicate<CommandSender> getExecutorPermission() {
        return this.executorPermission;
    }

    public void setExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
        this.executorPermission = executorPermission;
    }

    /**
     * Add default executor to define how to execute this command.
     *
     * for example :
     * <code>
     * this.addExecutor(... ,CommandArgument.ofString("example"),CommandArgument.ofString());
     * </code>
     * which means that it runs when you execute the command with "example" "xxx".
     *
     * <code>
     * this.addExecutor(...);
     * </code>
     * which means that it runs when you just execute the command without anything.
     *
     * @param executor         the executor to define this command
     * @param commandArguments the defined arguments for this executor
     * @return the Executor to define other proprieties
     */
    @NotNull
    public final Executor addExecutor(@NotNull final CommandExecutor executor, @NotNull final CommandArgument<?>... commandArguments) {
        final Executor executor1 = new Executor(executor, this.executorPermission, this, commandArguments);
        this.executors.add(executor1);
        return executor1;
    }

    /**
     * Execute the command with special arguments
     *
     * @param sender    the executor
     * @param args      the arguments that command spilt by spaces
     * @param ioHandler the receiver
     * @return the command result
     *
     * @throws IllegalArgumentException internal error, never expected
     * @throws Exception the exception that occurred when executing the command
     */
    public final CommandResult execute(@NotNull final CommandSender sender, @NotNull final String[] args,@NotNull IOHandler ioHandler) throws Exception {
        if (!this.isRegistered())
            return CommandResult.COMMAND_REFUSED;
        if (!sender.hasPermission(this.getPermission()))
            return CommandResult.COMMAND_REFUSED;
        boolean flag = false;
        CommandResult result = CommandResult.NONE;
        for (final Executor executor : this.executors)
            if (sender.hasPermission(executor.permission)) {
                final DataCollection dataCollection;
                if ((dataCollection = executor.check(args)) != null) {
                    Exception exception = null;
                    try {
                        result = executor.execute(sender, dataCollection,ioHandler);
                    } catch (final Exception e) {
                        result = CommandResult.REFUSE;
                        exception = e;
                    }
                    for (final CommandResult r : executor.results.keySet())
                        if ((r.getValue() & result.getValue()) != 0)
                            executor.results.get(r).execute(result);
                    flag = true;
                    if (exception != null)
                        throw exception;
                    break;
                }
            }
        if (this.executorPermission.test(sender)) {
            if (!flag) {
                this.infoUsage(sender, ioHandler);
                return CommandResult.ARGS;
            } else if (result == CommandResult.ARGS) {
                this.infoUsage(sender, ioHandler);
                return CommandResult.ARGS_EXECUTED;
            }
        }
        return result;
    }

    @NotNull
    public CommandPermission getPermission() {
        return this.permission;
    }

    /**
     * Set the default permission
     *
     * @param permission the target permission the command need
     */
    public void setPermission(final CommandPermission permission) {
        this.permission = permission;
    }

    /**
     * Used to initialize the command (the primary goal is to define the default executors)
     */
    public abstract void init();

    /**
     * Used to get help information when execute this command with wrong arguments or the executor returns {@link CommandResult#ARGS}
     *
     * @param sender the executor which need to get help information
     * @return the help information
     */
    @NotNull
    public abstract List<String> usage(CommandSender sender);

    public final void infoUsage(final CommandSender sender, @NotNull IOHandler ioHandler) {
        final List<String> usage = this.usage(sender);
        int pos = 0;
        final int targetPos = 7;
        StringBuilder stringBuilder = null;
        while (pos != usage.size()) {
            if (pos % targetPos == 0) {
                if (stringBuilder != null)
                    ioHandler.output(stringBuilder.toString());
                stringBuilder = new StringBuilder(usage.get(pos));
            } else stringBuilder.append('\n').append(usage.get(pos));
            pos++;
        }
        if (stringBuilder != null)
            ioHandler.output(stringBuilder.toString());
    }

    /**
     * This class is used to help define the executor of certain command.
     * There is some special methods used to give more details of this executor.
     */
    public static class Executor {
        private final Map<CommandResult, CommandResultExecutor> results = Maps.newHashMap();
        private final CommandExecutor executor;
        private final CommandArgument<?>[] commandArguments;
        private final Command command;
        private final int nullableCommandArguments;
        private CommandPermission permission = CommandPermission.MEMBER;
        private Predicate<CommandSender> executorPermission;

        private Executor(final CommandExecutor executor, final Predicate<CommandSender> executorPermission, final Command command, final CommandArgument<?>[] commandArguments) {
            this.executor = executor;
            this.executorPermission = executorPermission;
            this.command = command;
            this.commandArguments = commandArguments;
            this.nullableCommandArguments = (int) Arrays.stream(commandArguments).filter(CommandArgument::isNullable).count();
        }

        private CommandResult execute(final CommandSender sender, final DataCollection dataCollection, @NotNull IOHandler ioHandler) {
            if (!this.executorPermission.test(sender))
                return CommandResult.REFUSE;
            return this.executor.execute(sender, dataCollection,ioHandler);
        }


        /**
         * Set the executor Permission
         * (Only if the CommandSender has the command that this executor belongs to and this executor's permissions, this executor runs)
         *
         * @param permission the executor Mirai Permission
         * @return the Executor itself
         */
        @NotNull
        public Executor setPermission(@NotNull final CommandPermission permission) {
            this.permission = permission;
            return this;
        }


        /**
         * Set the executor of the special CommandResult after executing this Executor
         *
         * @param result   the target CommandResult
         * @param executor the executor of the special CommandResult
         * @return the Executor itself
         */
        @NotNull
        public Executor addCommandResultExecutor(@NotNull final CommandResult result, @NotNull final CommandResultExecutor executor) {
            this.results.put(result, executor);
            return this;
        }

        /**
         * Set the executor permission check for this Executor
         * When execute this Executor, it will check {@link Command#executorPermission} and the executorPermission
         *
         * @param executorPermission the executor permission check for this Executor
         * @return the Executor self
         */
        @NotNull
        public Executor setExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = this.executorPermission.and(executorPermission);
            return this;
        }

        /**
         * Remove the executor permission check for this Executor
         *
         * @return the Executor self
         */
        @NotNull
        public Executor removeExecutorPermission() {
            this.executorPermission = i -> true;
            return this;
        }

        /**
         * Set the executor permission check for this Executor
         * When execute this Executor, it will only check the executorPermission
         *
         * @param executorPermission the executor permission check for this Executor
         * @return the Executor self
         */
        @NotNull
        public Executor overrideExecutorPermission(@NotNull final Predicate<CommandSender> executorPermission) {
            this.executorPermission = executorPermission;
            return this;
        }

        /**
         * Get the command this Executor belongs to
         *
         * @return the command this Executor belongs to
         */
        public Command getCommand() {
            return this.command;
        }

        /**
         * Check if the arguments are valid
         *
         * @param args the arguments of the command
         * @return the data collection of the arguments, null if the arguments are invalid
         * @throws IllegalArgumentException internal error, never expected
         */
        @Nullable
        private DataCollection check(final String[] args) {
            if (args.length > this.commandArguments.length)
                return null;
            if (args.length < this.commandArguments.length - this.nullableCommandArguments)
                return null;
            final List<CommandArgument<?>> commandArgumentList = Lists.newArrayList();
            final boolean ret = this.dfsCheck(args, 0, 0, this.commandArguments.length - args.length, commandArgumentList);
            if (!ret)
                return null;
            final DataCollection dataCollection = new DataCollection(Arrays.stream(this.commandArguments).map(CommandArgument::getDataConverter).toArray(DataConverter[]::new));
            for (int i = 0; i < args.length; i++)
                commandArgumentList.get(i).put(dataCollection, args[i]);
            dataCollection.flip();
            return dataCollection;
        }

        private boolean dfsCheck(@NotNull final String[] args, final int indexOfArgs, final int index, final int nullableCommandArguments, final List<CommandArgument<?>> commandArgumentList) {
            if (indexOfArgs == args.length)
                return true;
            if (this.commandArguments[index].isNullable() && nullableCommandArguments > 0) {
                final boolean ret = this.dfsCheck(args, indexOfArgs, index + 1, nullableCommandArguments - 1, commandArgumentList);
                if (ret)
                    return true;
            }
            if (this.commandArguments[index].accept(args[indexOfArgs])) {
                commandArgumentList.add(this.commandArguments[index]);
                final boolean ret = this.dfsCheck(args, indexOfArgs + 1, index + 1, nullableCommandArguments, commandArgumentList);
                if (ret)
                    return true;
                commandArgumentList.remove(commandArgumentList.size() - 1);
            }
            return false;
        }
    }
}
