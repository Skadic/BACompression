package utils;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class SparseArrayListTest {


    SparseArrayList<String> testList;
    List<String> otherList;

    @BeforeEach
    void setUp() {
        testList = new SparseArrayList<>(10);
        otherList = new ArrayList<>(10);

        for (int i = 0; i < 10; i++) {
            testList.add(Character.toString('a' + i));
            otherList.add(Character.toString('a' + i));
        }
    }


    @Test
    void testCreate() {
        Assertions.assertEquals(otherList, testList);
    }


    @Test
    void iterTest() {
        int i = 0;
        for (String s : testList) {
            Assertions.assertEquals(otherList.get(i++), s);
            System.out.print(s);
        }
    }

    @Test
    void clearTest() {
        Assertions.assertEquals(otherList, testList);

        testList.subList(2, 4).clear();
        otherList.subList(2, 4).clear();

        Assertions.assertEquals(otherList, testList, "Unequal after first clear");

        testList.subList(2, 4).clear();
        otherList.subList(2, 4).clear();

        Assertions.assertEquals(otherList, testList, "Unequal after overlapping clear");
    }

    @Test
    void addTest() {

        testList.add("x");
        Assertions.assertEquals("x", testList.get(testList.size() - 1));
        Assertions.assertEquals(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "x"), testList);
        testList.subList(2, 4).clear();
        Assertions.assertEquals(List.of("a", "b", "e", "f", "g", "h", "i", "j", "x"), testList);
        testList.add("y");
        Assertions.assertEquals(List.of("a", "b", "e", "f", "g", "h", "i", "j", "x", "y"), testList);

        testList.iterator();
    }


}
