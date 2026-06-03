package top.focess.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataCollectionTest {

    @Test
    void storesAndReadsValuesInOrder() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.INTEGER_DATA_CONVERTER,
                DataConverter.INTEGER_DATA_CONVERTER,
                DataConverter.DEFAULT_DATA_CONVERTER
        });
        collection.write(Integer.class, 1);
        collection.write(Integer.class, 2);
        collection.write(String.class, "hello");
        collection.flip();

        assertEquals(1, collection.getInt());
        assertEquals(2, collection.getInt());
        assertEquals("hello", collection.get());
    }

    @Test
    void readsValuesByIndex() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.LONG_DATA_CONVERTER,
                DataConverter.LONG_DATA_CONVERTER
        });
        collection.write(Long.class, 10L);
        collection.write(Long.class, 20L);
        collection.flip();

        assertEquals(10L, collection.get(Long.class, 0));
        assertEquals(20L, collection.get(Long.class, 1));
    }

    @Test
    void getOrDefaultThrowsForUnregisteredClass() {
        final DataCollection collection = new DataCollection(new DataConverter<?>[]{
                DataConverter.DEFAULT_DATA_CONVERTER
        });
        collection.write(String.class, "value");
        collection.flip();

        assertThrows(IllegalStateException.class, () -> collection.getOrDefault(Boolean.class, true));
    }
}
