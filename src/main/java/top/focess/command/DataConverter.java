package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the conversion of raw String input into typed objects.
 * <p>
 * {@code DataConverter} is the heart of the framework's type-safe argument system. It defines 
 * how a string token (from the command line) is validated, converted, and stored in a 
 * {@link DataCollection}.
 * 
 * <h2>Conversion Lifecycle</h2>
 * <ol>
 *   <li>{@link #accept(String)}: Validates if the raw string matches the expected format.</li>
 *   <li>{@link #convert(String)}: Transforms the validated string into a Java object of type {@code T}.</li>
 *   <li>{@link #getTargetClass()}: Specifies the storage class used by {@link DataCollection}.</li>
 * </ol>
 * 
 * <h2>Extensibility</h2>
 * Subclasses can override {@link #complete(CommandSender, String)} to provide custom 
 * tab-completions specific to the data type.
 *
 * @param <T> The target Java type this converter produces.
 */
public abstract class DataConverter<T> {

    /**
     * Constructs a new {@code DataConverter}.
     */
    protected DataConverter() {
    }

    /**
     * Matches an optionally-signed sequence of digits (a candidate integral value).
     */
    private static final Pattern INTEGRAL_PATTERN = Pattern.compile("[+-]?\\d+");

    /**
     * Matches a candidate floating-point value (decimal or scientific notation).
     */
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?[fFdD]?");

    /**
     * It is a Predicate used to predicate a String is an Integer.
     * <p>
     * The regex is only a fast pre-filter; {@link Integer#parseInt(String)} remains the basis so that
     * out-of-range values (which match the regex but overflow) are still rejected.
     */
    public static final Predicate<String> INTEGER_PREDICATE = i -> {
        if (i == null || !INTEGRAL_PATTERN.matcher(i).matches())
            return false;
        try {
            Integer.parseInt(i);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    };

    /**
     * It is a Predicate used to predicate a String is a Double.
     * <p>
     * The regex is only a fast pre-filter; {@link Double#parseDouble(String)} remains the basis.
     */
    public static final Predicate<String> DOUBLE_PREDICATE = i -> {
        if (i == null || !DECIMAL_PATTERN.matcher(i).matches())
            return false;
        try {
            Double.parseDouble(i);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    };

    /**
     * It is a Predicate used to predicate a String is a Long.
     * <p>
     * The regex is only a fast pre-filter; {@link Long#parseLong(String)} remains the basis so that
     * out-of-range values (which match the regex but overflow) are still rejected.
     */
    public static final Predicate<String> LONG_PREDICATE = i -> {
        if (i == null || !INTEGRAL_PATTERN.matcher(i).matches())
            return false;
        try {
            Long.parseLong(i);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    };


    /**
     * Never convert it! Put them into DataCollection with their original values.
     */
    public static final DataConverter<String> DEFAULT_DATA_CONVERTER = new DataConverter<String>() {
        @Override
        public boolean accept(final String arg) {
            return true;
        }

        @Override
        public String convert(final String arg) {
            return arg;
        }

        @Override
        protected Class<String> getTargetClass() {
            return String.class;
        }
    };


    /**
     * Convert the String argument to Integer argument
     */
    public static final DataConverter<Integer> INTEGER_DATA_CONVERTER = new DataConverter<Integer>() {
        @Override
        public boolean accept(final String arg) {
            return INTEGER_PREDICATE.test(arg);
        }

        @NotNull
        @Override
        public Integer convert(final String arg) {
            return Integer.parseInt(arg);
        }

        @Override
        protected Class<Integer> getTargetClass() {
            return Integer.class;
        }
    };

    /**
     * Convert the String argument to Long argument
     */
    public static final DataConverter<Long> LONG_DATA_CONVERTER = new DataConverter<Long>() {
        @Override
        public boolean accept(final String arg) {
            return LONG_PREDICATE.test(arg);
        }

        @NotNull
        @Override
        public Long convert(final String arg) {
            return Long.parseLong(arg);
        }

        @Override
        protected Class<Long> getTargetClass() {
            return Long.class;
        }
    };

    /**
     * Convert the String argument to Double argument
     */
    public static final DataConverter<Double> DOUBLE_DATA_CONVERTER = new DataConverter<Double>() {
        @Override
        public boolean accept(final String s) {
            return DOUBLE_PREDICATE.test(s);
        }

        @NotNull
        @Override
        public Double convert(final String s) {
            return Double.parseDouble(s);
        }

        @Override
        protected Class<Double> getTargetClass() {
            return Double.class;
        }
    };

    /**
     * Convert the String argument to Boolean argument
     */
    public static final DataConverter<Boolean> BOOLEAN_DATA_CONVERTER = new DataConverter<Boolean>() {
        @Override
        public boolean accept(final @NotNull String arg) {
            return arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false");
        }

        @Contract(pure = true)
        @Override
        public @NotNull Boolean convert(final String arg) {
            return Boolean.parseBoolean(arg);
        }

        @Override
        @NotNull
        public List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final String arg) {
            final List<String> choices = List.of("true", "false");
            if (arg.isEmpty())
                return choices.stream().map(CommandCompletion::of).collect(Collectors.toList());
            return choices.stream()
                    .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                    .map(CommandCompletion::of)
                    .collect(Collectors.toList());
        }

        @Override
        protected Class<Boolean> getTargetClass() {
            return Boolean.class;
        }
    };

    /**
     * Create a DataConverter that suggests fixed choices.
     *
     * @param choices the available choices
     * @return the DataConverter
     */
    @NotNull
    @Contract("_ -> new")
    public static DataConverter<String> ofChoices(@NotNull final String... choices) {
        return ofChoices(Arrays.asList(choices));
    }

    /**
     * Create a DataConverter that suggests fixed choices with descriptions.
     *
     * @param choices the available choices (candidate -> description)
     * @return the DataConverter
     */
    @NotNull
    @Contract("_ -> new")
    public static DataConverter<String> ofChoices(@NotNull final Map<String, String> choices) {
        return new DataConverter<String>() {
            @Override
            public boolean accept(String arg) {
                return choices.containsKey(arg);
            }

            @Override
            public String convert(String arg) {
                return arg;
            }

            @Override
            @NotNull
            public List<CommandCompletion> complete(@NotNull CommandSender sender, @NotNull String arg) {
                if (arg.isEmpty())
                    return choices.entrySet().stream()
                            .map(entry -> CommandCompletion.of(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());
                return choices.entrySet().stream()
                        .filter(entry -> entry.getKey().toLowerCase().startsWith(arg.toLowerCase()))
                        .map(entry -> CommandCompletion.of(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
            }

            @Override
            protected Class<String> getTargetClass() {
                return String.class;
            }
        };
    }

    /**
     * Create a DataConverter that suggests fixed choices.
     *
     * @param choices the available choices
     * @return the DataConverter
     */
    @NotNull
    @Contract("_ -> new")
    public static DataConverter<String> ofChoices(@NotNull final List<String> choices) {
        return new DataConverter<String>() {
            @Override
            public boolean accept(String arg) {
                return choices.contains(arg);
            }

            @Override
            public String convert(String arg) {
                return arg;
            }

            @Override
            @NotNull
            public List<CommandCompletion> complete(@NotNull CommandSender sender, @NotNull String arg) {
                if (arg.isEmpty())
                    return choices.stream().map(CommandCompletion::of).collect(Collectors.toList());
                return choices.stream()
                        .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                        .map(CommandCompletion::of)
                        .collect(Collectors.toList());
            }

            @Override
            protected Class<String> getTargetClass() {
                return String.class;
            }
        };
    }

    /**
     * Create a DataConverter for an Enum class.
     *
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the DataConverter
     */
    @NotNull
    @Contract("_ -> new")
    public static <E extends Enum<E>> DataConverter<E> ofEnum(@NotNull final Class<E> enumClass) {
        return ofEnum(enumClass, e -> null);
    }

    /**
     * Create a DataConverter for an Enum class with custom descriptions.
     *
     * @param enumClass           the enum class
     * @param descriptionFunction the function to get the description for an enum constant
     * @param <E>                 the enum type
     * @return the DataConverter
     */
    @NotNull
    @Contract("_, _ -> new")
    public static <E extends Enum<E>> DataConverter<E> ofEnum(@NotNull final Class<E> enumClass, @NotNull final Function<E, String> descriptionFunction) {
        return new DataConverter<E>() {
            @Override
            public boolean accept(String arg) {
                for (final E e : enumClass.getEnumConstants())
                    if (e.name().equalsIgnoreCase(arg))
                        return true;
                return false;
            }

            @Override
            public E convert(String arg) {
                for (final E e : enumClass.getEnumConstants())
                    if (e.name().equalsIgnoreCase(arg))
                        return e;
                throw new IllegalArgumentException("No enum constant " + enumClass.getCanonicalName() + "." + arg);
            }

            @Override
            @NotNull
            public List<CommandCompletion> complete(@NotNull CommandSender sender, @NotNull String arg) {
                if (arg.isEmpty())
                    return Arrays.stream(enumClass.getEnumConstants())
                            .map(e -> CommandCompletion.of(e.name().toLowerCase(), descriptionFunction.apply(e)))
                            .collect(Collectors.toList());
                return Arrays.stream(enumClass.getEnumConstants())
                        .filter(e -> e.name().toLowerCase().startsWith(arg.toLowerCase()))
                        .map(e -> CommandCompletion.of(e.name().toLowerCase(), descriptionFunction.apply(e)))
                        .collect(Collectors.toList());
            }

            @Override
            protected Class<E> getTargetClass() {
                return enumClass;
            }
        };
    }

    /**
     * Determines if the given string argument is valid for this converter.
     *
     * @param arg The raw string token from the command line.
     * @return {@code true} if the argument can be converted by {@link #convert(String)}, 
     *         {@code false} otherwise.
     */
    public abstract boolean accept(String arg);

    /**
     * Transforms a raw string into a typed object.
     * <p>
     * <b>Note:</b> This method is only called if {@link #accept(String)} returns {@code true}.
     *
     * @param arg The raw string token to convert.
     * @return The converted object of type {@code T}.
     */
    public abstract T convert(String arg);

    /**
     * Internal framework method to validate, convert, and store an argument.
     */
    boolean put(final DataCollection dataCollection, final String arg) {
        if (this.accept(arg)) {
            this.connect(dataCollection, this.convert(arg));
            return true;
        }
        return false;
    }

    /**
     * Internal framework method to write a typed value into the data collection.
     */
    void connect(@NotNull final DataCollection dataCollection, final T arg) {
        dataCollection.write(this.getTargetClass(), arg);
    }

    /**
     * Returns the runtime class of the target type {@code T}.
     * <p>
     * This is used by {@link DataCollection} for type-safe retrieval.
     * 
     * @return The {@link Class} object for {@code T}.
     */
    protected abstract Class<T> getTargetClass();

    /**
     * Generates tab-completion suggestions for this specific data type.
     * <p>
     * The default implementation returns an empty list. Override this to provide
     * meaningful suggestions (e.g., player names, file paths, or enum constants).
     *
     * @param sender The entity requesting completions.
     * @param arg    The partial string currently being typed.
     * @return A non-null list of completions.
     */
    @NotNull
    public List<CommandCompletion> complete(@NotNull final CommandSender sender, @NotNull final String arg) {
        return List.of();
    }
}
