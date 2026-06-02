package top.focess.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataConverterTest {

    @Test
    void integerConverterAcceptsAndConverts() {
        assertTrue(DataConverter.INTEGER_DATA_CONVERTER.accept("42"));
        assertFalse(DataConverter.INTEGER_DATA_CONVERTER.accept("4.2"));
        assertFalse(DataConverter.INTEGER_DATA_CONVERTER.accept("foo"));
        assertEquals(42, DataConverter.INTEGER_DATA_CONVERTER.convert("42"));
    }

    @Test
    void longConverterAcceptsAndConverts() {
        assertTrue(DataConverter.LONG_DATA_CONVERTER.accept("9999999999"));
        assertFalse(DataConverter.LONG_DATA_CONVERTER.accept("bar"));
        assertEquals(9999999999L, DataConverter.LONG_DATA_CONVERTER.convert("9999999999"));
    }

    @Test
    void doubleConverterAcceptsAndConverts() {
        assertTrue(DataConverter.DOUBLE_DATA_CONVERTER.accept("3.14"));
        assertFalse(DataConverter.DOUBLE_DATA_CONVERTER.accept("pi"));
        assertEquals(3.14, DataConverter.DOUBLE_DATA_CONVERTER.convert("3.14"));
    }

    @Test
    void booleanConverterIsCaseInsensitive() {
        assertTrue(DataConverter.BOOLEAN_DATA_CONVERTER.accept("TRUE"));
        assertTrue(DataConverter.BOOLEAN_DATA_CONVERTER.accept("false"));
        assertFalse(DataConverter.BOOLEAN_DATA_CONVERTER.accept("yes"));
        assertTrue(DataConverter.BOOLEAN_DATA_CONVERTER.convert("true"));
    }

    @Test
    void defaultConverterAcceptsAnything() {
        assertTrue(DataConverter.DEFAULT_DATA_CONVERTER.accept("anything"));
        assertEquals("anything", DataConverter.DEFAULT_DATA_CONVERTER.convert("anything"));
    }
}
