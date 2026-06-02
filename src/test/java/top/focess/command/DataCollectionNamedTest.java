package top.focess.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCollectionNamedTest {

    @Test
    void readsValuesByName() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.INTEGER_DATA_CONVERTER,
                DataConverter.DEFAULT_DATA_CONVERTER
        });
        final CommandArgument<Integer> count = CommandArgument.ofInt().named("count");
        final CommandArgument<String> message = CommandArgument.ofString().named("message");

        count.put(collection, "7");
        message.put(collection, "hello");
        collection.flip();

        assertEquals(7, (int) collection.get("count"));
        assertEquals("hello", collection.get("message"));
    }

    @Test
    void getByNameThrowsForUnknownName() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.DEFAULT_DATA_CONVERTER
        });
        CommandArgument.ofString().named("present").put(collection, "value");
        collection.flip();

        assertThrows(UnsupportedOperationException.class, () -> collection.get("absent"));
        assertEquals("fallback", collection.getOrDefault("absent", "fallback"));
        assertEquals("value", collection.getOrDefault("present", "fallback"));
    }

    @Test
    void namedAccessCoexistsWithPositionalAccess() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.INTEGER_DATA_CONVERTER
        });
        CommandArgument.ofInt().named("value").put(collection, "42");
        collection.flip();

        assertEquals(42, collection.getInt());
        assertEquals(42, (int) collection.get("value"));
    }

    @Test
    void integerPredicateRejectsOverflowButRegexCandidate() {
        // matches the integral regex but overflows int; the parser remains the basis
        assertFalse(DataConverter.INTEGER_PREDICATE.test("99999999999999999999"));
        assertTrue(DataConverter.INTEGER_PREDICATE.test("+42"));
        assertTrue(DataConverter.INTEGER_PREDICATE.test("-42"));
        assertFalse(DataConverter.INTEGER_PREDICATE.test("4.2"));
        // matches the integral regex but overflows long
        assertFalse(DataConverter.LONG_PREDICATE.test("99999999999999999999"));
        assertTrue(DataConverter.DOUBLE_PREDICATE.test("3.14e2"));
        assertFalse(DataConverter.DOUBLE_PREDICATE.test("3.1.4"));
    }
}
