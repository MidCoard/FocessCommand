package top.focess.command;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single argument definition within a command signature.
 * <p>
 * A {@code CommandArgument} defines the metadata, validation logic, and completion 
 * behavior for a specific positional token in a command string. 
 * 
 * <h2>Types of Arguments</h2>
 * <ul>
 *   <li><b>Variable Arguments:</b> Defined by a {@link DataConverter}. They accept any string 
 *       that the converter validates (e.g., an integer or a player name).</li>
 *   <li><b>Fixed Literals:</b> Created via {@link #of(String)}. They only accept a 
 *       specific, case-insensitive string value (e.g., the word "create" in "/party create").</li>
 *   <li><b>Optional Arguments:</b> Created via {@link #ofNullable(DataConverter)}. These can 
 *       be omitted by the user. If omitted, they are stored as {@code null} in 
 *       {@link DataCollection}.</li>
 * </ul>
 * 
 * <h2>Naming and Retrieval</h2>
 * Use {@link #named(String)} to assign a key to the argument. This allows the 
 * {@link CommandExecutor} to retrieve the parsed value by name (e.g., {@code data.get("target")}) 
 * instead of just by index.
 *
 * @param <V> The Java type that this argument resolves to after parsing.
 */
public class CommandArgument<V> {

    private final DataConverter<V> dataConverter;
    private final V value;
    private final boolean isNullable;
    @Nullable
    private String name;
    @Nullable
    private String description;

    private CommandArgument(@NotNull final DataConverter<V> dataConverter, @Nullable final V value) {
        this.dataConverter = dataConverter;
        this.value = value;
        this.isNullable = false;
    }

    private CommandArgument(@NotNull final DataConverter<V> dataConverter, final boolean isNullable) {
        this.dataConverter = dataConverter;
        this.value = null;
        this.isNullable = isNullable;
    }

    @Nullable
    private CommandCompleter completer;

    /**
     * Set a custom completer for this argument.
     *
     * @param completer the completer
     * @return this argument, for chaining
     */
    @NotNull
    public CommandArgument<V> completer(@NotNull final CommandCompleter completer) {
        this.completer = completer;
        return this;
    }

    /**
     * Get the auto-complete suggestions for this argument.
     * <p>
     * The {@code args} array must contain the command's arguments as produced by a tokenizer.
     * It should NOT include the command name itself. The last element is treated as the 
     * partial argument currently being completed.
     * 
     * <h2>Example Usage</h2>
     * <pre>{@code
     * String input = "party create ";
     * String[] args = CommandManager.tokenizeToCommandArgs(input);
     * List<CommandCompletion> suggestions = argument.complete(sender, command, args);
     * }</pre>
     * 
     * @param sender  the executor
     * @param command the command
     * @param args    the tokenized arguments following the command name. 
     *                See {@link CommandManager#tokenizeToCommandArgs(String)} for the expected format.
     * @return the auto-complete suggestions
     */
    @NotNull
    public List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String[] args) {
        if (this.completer != null)
            return this.completer.complete(sender, command, args);
        final String arg = args.length == 0 ? "" : args[args.length - 1];
        if (this.isFixed()) {
            final String valueString = String.valueOf(this.value);
            if (valueString.toLowerCase().startsWith(arg.toLowerCase()))
                return Collections.singletonList(CommandCompletion.of(valueString, this.description));
            return Collections.emptyList();
        }
        return this.dataConverter.complete(sender, arg);
    }

    /**
     * Represent an unknown String CommandArgument

     * Note: this argument indicates this position need a String value.
     *
     * @return the CommandArgument representing an unknown String
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static CommandArgument<String> ofString() {
        return new CommandArgument<>(DataConverter.DEFAULT_DATA_CONVERTER, null);
    }

    /**
     * Represent an unknown Long CommandArgument

     * Note: this argument indicates this position need a Long value.
     *
     * @return the CommandArgument representing an unknown Long
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static CommandArgument<Long> ofLong() {
        return new CommandArgument<>(DataConverter.LONG_DATA_CONVERTER, null);
    }

    /**
     * Represents an unknown Int CommandArgument

     * Note: this argument indicates this position need an Int value.
     *
     * @return the CommandArgument representing an unknown Int
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static CommandArgument<Integer> ofInt() {
        return new CommandArgument<>(DataConverter.INTEGER_DATA_CONVERTER, null);
    }

    /**
     * Represents an unknown CommandArgument with a specific DataConverter

     * Note: this argument indicates this position need a V type value
     *
     * @param defaultDataConverter the DataConverter
     * @param <V> the type of the argument.
     * @return the CommandArgument representing an unknown CommandArgument with a specific DataConverter
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <V> CommandArgument<V> of(final DataConverter<V> defaultDataConverter) {
        return new CommandArgument<>(defaultDataConverter, null);
    }

    /**
     * Represents a CommandArgument with a specific String value

     * Note: this argument indicates this position is a known String value.
     *
     * @param value the String value of the CommandArgument
     * @return the CommandArgument with a specific String value
     */
    @NotNull
    public static CommandArgument<String> of(@NotNull final String value) {
        return new CommandArgument<>(DataConverter.DEFAULT_DATA_CONVERTER, value);
    }

    /**
     * Represents a CommandArgument with a specific value

     * Note: this argument indicates this position is a known V value.
     *
     * @param dataConverter the DataConverter
     * @param value the value of the CommandArgument
     * @param <V> the type of the argument.
     * @return the CommandArgument with a specific value
     */
    @NotNull
    public static <V> CommandArgument<V> of(@NotNull final DataConverter<V> dataConverter, @NotNull final V value) {
        return new CommandArgument<>(dataConverter, value);
    }

    /**
     * Represents a nullable CommandArgument with a specific DataConverter

     * Note: this argument indicates this position is a nullable value.
     *
     * @param dataConverter the DataConverter
     * @param <V> the type of the argument.
     * @return the nullable CommandArgument with a specific DataConverter
     */
    @NotNull
    public static <V> CommandArgument<V> ofNullable(@NotNull final DataConverter<V> dataConverter) {
        return new CommandArgument<>(dataConverter, true);
    }

    /**
     * Check if this argument is nullable (optional).
     *
     * @return true if nullable, false otherwise
     */
    public boolean isNullable() {
        return this.isNullable;
    }

    /**
     * Assign a name to this argument so that its parsed value can be looked up by name from the
     * {@link DataCollection} (via {@link DataCollection#get(String)}), in addition to positional/typed
     * access. Naming is ignored for fixed-literal arguments (created from a known value).
     *
     * @param name the name of the argument
     * @return this argument, for chaining
     */
    @NotNull
    public CommandArgument<V> named(@NotNull final String name) {
        this.name = name;
        return this;
    }

    /**
     * Assign a description to this argument.
     *
     * @param description the description of the argument
     * @return this argument, for chaining
     */
    @NotNull
    public CommandArgument<V> description(@NotNull final String description) {
        this.description = description;
        return this;
    }

    /**
     * Get the name of this argument.
     *
     * @return the name, or null if not set
     */
    @Nullable
    public String getName() {
        return this.name;
    }

    /**
     * Get the description of this argument.
     *
     * @return the description, or null if not set
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Check if this is a fixed-literal argument (created from a known value).
     *
     * @return true if fixed, false otherwise
     */
    public boolean isFixed() {
        return this.value != null;
    }

    @Nullable
    V getValue() {
        return this.value;
    }

    DataConverter<V> getDataConverter() {
        return this.dataConverter;
    }

    boolean accept(final String arg) {
        if (this.isFixed())
            return this.getDataConverter().accept(arg) && Objects.equals(this.getValue(), this.getDataConverter().convert(arg));
        else return this.getDataConverter().accept(arg);
    }

    /**
     * Put the value of the CommandArgument in the DataCollection
     * @param dataCollection the DataCollection
     * @param arg the value of the CommandArgument
     * @throws IllegalArgumentException if the value is not accepted by the DataConverter
     */
    void put(final DataCollection dataCollection, final String arg) {
        if (!this.isFixed())
            if (!this.getDataConverter().put(dataCollection, arg))
                throw new IllegalArgumentException("The argument " + arg + " is not valid for this argument");
        if (this.name != null && this.getDataConverter().accept(arg))
            dataCollection.writeNamed(this.name, this.getDataConverter().convert(arg));
    }

}
