package top.focess.command;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import top.focess.command.converter.ExceptionDataConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class used to convert String data to target T type data.
 *
 * @param <T> target type
 */
public abstract class DataConverter<T> {

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
        public List<String> complete(@NotNull final CommandSender sender, @NotNull final String arg) {
            final List<String> choices = List.of("true", "false");
            if (arg.isEmpty())
                return choices;
            return choices.stream()
                    .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
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
            public List<String> complete(@NotNull CommandSender sender, @NotNull String arg) {
                if (arg.isEmpty())
                    return choices;
                return choices.stream()
                        .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
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
            public List<String> complete(@NotNull CommandSender sender, @NotNull String arg) {
                final List<String> names = Arrays.stream(enumClass.getEnumConstants())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                if (arg.isEmpty())
                    return names;
                return names.stream()
                        .filter(s -> s.startsWith(arg.toLowerCase()))
                        .collect(Collectors.toList());
            }

            @Override
            protected Class<E> getTargetClass() {
                return enumClass;
            }
        };
    }

    /**
     * Indicate whether this String argument is this target type or not
     *
     * @param arg the target argument in String
     * @return true if this String argument can convert to this target type, false otherwise
     */
    public abstract boolean accept(String arg);

    /**
     * Convert String argument to target argument
     *
     * Note: this method is called only when {@link #accept(String)} return true
     *
     * @param arg the target argument in String
     * @return the target argument
     */
    public abstract T convert(String arg);

    boolean put(final DataCollection dataCollection, final String arg) {
        if (this.accept(arg)) {
            this.connect(dataCollection, this.convert(arg));
            return true;
        }
        return false;
    }

    void connect(@NotNull final DataCollection dataCollection, final T arg) {
        dataCollection.write(this.getTargetClass(), arg);
    }

    protected abstract Class<T> getTargetClass();

    /**
     * Get the auto-complete suggestions for this argument
     *
     * @param sender the executor
     * @param arg    the current argument
     * @return the auto-complete suggestions
     */
    @NotNull
    public List<String> complete(@NotNull final CommandSender sender, @NotNull final String arg) {
        return List.of();
    }
}
