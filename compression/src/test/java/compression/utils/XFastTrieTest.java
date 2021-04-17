package compression.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class XFastTrieTest {

    XFastTrie<String> trie;

    @BeforeEach
    void setUp() {
        trie = new XFastTrie<>();
        trie.put(1, "Go");
        trie.put(3, "Yey");
        trie.put(5, "Hi");

        trie.put(25, "nice");
        System.out.println();
    }

    @Test
    void testGet() {
        Assertions.assertNull(trie.get(0), "Value returned, despite key not being mapped");
        Assertions.assertEquals("Go", trie.get(1));
        Assertions.assertEquals("Hi", trie.get(5));
        Assertions.assertEquals("Yey", trie.get(3));
        Assertions.assertEquals("nice", trie.get(25));
    }

    @Test
    void testFloor() {
        Assertions.assertNull(trie.floorEntry(0));
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(1, "Go"), trie.floorEntry(1), "when passing 1");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(1, "Go"), trie.floorEntry(2), "when passing 2");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(3, "Yey"), trie.floorEntry(3), "when passing 3");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(3, "Yey"), trie.floorEntry(4), "when passing 4");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(25, "nice"), trie.floorEntry(100), "when passing 100");
    }

    @Test
    void testCeil() {
        Assertions.assertNull(trie.floorEntry(0));
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(1, "Go"), trie.ceilingEntry(1), "when passing 1");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(3, "Yey"), trie.ceilingEntry(2), "when passing 2");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(3, "Yey"), trie.ceilingEntry(3), "when passing 3");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(5, "Hi"), trie.ceilingEntry(4), "when passing 4");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(25, "nice"), trie.ceilingEntry(6), "when passing 6");
        Assertions.assertEquals(new IntPredecessor.IntEntry<>(25, "nice"), trie.ceilingEntry(25), "when passing 25");
        Assertions.assertNull(trie.ceilingEntry(26), "when passing 26");
    }

    @Test
    void testRemove() {
        Assertions.assertNull(trie.remove(2), "Did not return null despite not removing");
        Assertions.assertEquals("Yey", trie.remove(3));
        Assertions.assertNull(trie.get(3));
    }

    @Test
    void testValues() {
        Assertions.assertEquals(List.of("Go", "Yey", "Hi", "nice"), trie.values());
    }

    @Test
    void testValueRange() {
        Assertions.assertEquals(List.of("Go", "Yey", "Hi", "nice"), trie.valueRange(1, true, 25, true));
        Assertions.assertEquals(List.of("Yey", "Hi", "nice"), trie.valueRange(1, false, 25, true));
        Assertions.assertEquals(List.of("Go", "Yey", "Hi"), trie.valueRange(1, true, 25, false));
        Assertions.assertEquals(List.of("Yey", "Hi"), trie.valueRange(1, false, 25, false));
    }
}
