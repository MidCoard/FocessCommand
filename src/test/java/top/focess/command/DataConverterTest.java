package top.focess.command;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    private enum Color { RED, GREEN, BLUE }

    @Test
    void ofEnumIsCaseInsensitive() {
        DataConverter<Color> converter = DataConverter.ofEnum(Color.class);
        assertTrue(converter.accept("RED"));
        assertTrue(converter.accept("green"));
        assertFalse(converter.accept("yellow"));
        assertEquals(Color.RED, converter.convert("red"));
        assertEquals(Color.BLUE, converter.convert("BLUE"));
    }

    @Test
    void ofChoicesSuggestsCorrectly() {
        DataConverter<String> converter = DataConverter.ofChoices("apple", "banana");
        CommandSender sender = new CommandSender(CommandPermission.MEMBER) {};
        
        List<String> suggestions = converter.complete(sender, "");
        assertTrue(suggestions.contains("apple"));
        assertTrue(suggestions.contains("banana"));

        suggestions = converter.complete(sender, "app");
        assertTrue(suggestions.contains("apple"));
        assertFalse(suggestions.contains("banana"));
    }

    @Test
    void booleanConverterSuggestsCorrectly() {
        CommandSender sender = new CommandSender(CommandPermission.MEMBER) {};
        List<String> suggestions = DataConverter.BOOLEAN_DATA_CONVERTER.complete(sender, "");
        assertTrue(suggestions.contains("true"));
        assertTrue(suggestions.contains("false"));

        suggestions = DataConverter.BOOLEAN_DATA_CONVERTER.complete(sender, "t");
        assertTrue(suggestions.contains("true"));
        assertFalse(suggestions.contains("false"));
    }
}
