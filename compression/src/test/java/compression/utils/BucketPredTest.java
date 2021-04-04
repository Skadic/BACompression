package compression.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BucketPredTest {

    BucketPred<String> pred;

    @BeforeEach
    void setUp() {
        pred = new BucketPred<>(200, 4);
        pred.put(1, "Go");
        pred.put(3, "Yey");
        pred.put(5, "Hi");

        pred.put(25, "nice");
        System.out.println();
    }

    @Test
    void testGet() {
        Assertions.assertNull(pred.get(0), "Value returned, despite key not being mapped");
        Assertions.assertEquals("Go", pred.get(1));
        Assertions.assertEquals("Hi", pred.get(5));
        Assertions.assertEquals("Yey", pred.get(3));
        Assertions.assertEquals("nice", pred.get(25));
    }

    @Test
    void testFloor() {
        Assertions.assertNull(pred.floorEntry(0));
        Assertions.assertEquals(new Predecessor.Entry<>(1, "Go"), pred.floorEntry(1), "when passing 1");
        Assertions.assertEquals(new Predecessor.Entry<>(1, "Go"), pred.floorEntry(2), "when passing 2");
        Assertions.assertEquals(new Predecessor.Entry<>(3, "Yey"), pred.floorEntry(3), "when passing 3");
        Assertions.assertEquals(new Predecessor.Entry<>(3, "Yey"), pred.floorEntry(4), "when passing 4");
        Assertions.assertEquals(new Predecessor.Entry<>(25, "nice"), pred.floorEntry(100), "when passing 100");
    }

    @Test
    void testCeil() {
        Assertions.assertNull(pred.floorEntry(0));
        Assertions.assertEquals(new XFastTrie.Entry<>(1, "Go"), pred.ceilingEntry(1), "when passing 1");
        Assertions.assertEquals(new XFastTrie.Entry<>(3, "Yey"), pred.ceilingEntry(2), "when passing 2");
        Assertions.assertEquals(new XFastTrie.Entry<>(3, "Yey"), pred.ceilingEntry(3), "when passing 3");
        Assertions.assertEquals(new XFastTrie.Entry<>(5, "Hi"), pred.ceilingEntry(4), "when passing 4");
        Assertions.assertEquals(new XFastTrie.Entry<>(25, "nice"), pred.ceilingEntry(6), "when passing 6");
        Assertions.assertEquals(new XFastTrie.Entry<>(25, "nice"), pred.ceilingEntry(25), "when passing 25");
        Assertions.assertNull(pred.ceilingEntry(26), "when passing 26");
    }

    @Test
    void testRemove() {
        Assertions.assertNull(pred.remove(2), "Did not return null despite not removing");
        Assertions.assertEquals("Yey", pred.remove(3));
        Assertions.assertNull(pred.get(3));
    }

    @Test
    void testValues() {
        Assertions.assertEquals(List.of("Go", "Yey", "Hi", "nice"), pred.values());
    }

    @Test
    void testValueRange() {
        Assertions.assertEquals(List.of("Go", "Yey", "Hi", "nice"), pred.valueRange(1, true, 25, true));
        Assertions.assertEquals(List.of("Yey", "Hi", "nice"), pred.valueRange(1, false, 25, true));
        Assertions.assertEquals(List.of("Go", "Yey", "Hi"), pred.valueRange(1, true, 25, false));
        Assertions.assertEquals(List.of("Yey", "Hi"), pred.valueRange(1, false, 25, false));
    }

    @Test
    void testOutOfBounds() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pred.get(-1), "Did not throw out of bounds exception despite index being -1");
        Assertions.assertDoesNotThrow(() -> pred.get(0), "Threw exception at index 0");
        Assertions.assertDoesNotThrow(() -> pred.get(199), "Threw exception at index 0");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pred.get(200), "Did not throw out of bounds exception despite index being 200");
    }

    @Test
    void testPutReplace() {
        Assertions.assertEquals("Go", pred.get(1));
        Assertions.assertEquals("Go", pred.put(1, "Stop"), "Did not return old value of index 1 upon inserting new value");
        Assertions.assertEquals("Stop", pred.get(1), "Value was not replaced");
    }

}
