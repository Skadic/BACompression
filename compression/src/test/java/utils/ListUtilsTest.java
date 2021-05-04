package utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ListUtilsTest {

    private List<Integer> list;

    @BeforeEach
    void beforeEach() {
        list = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            list.add(i);
        }
    }

    @Test
    void clearRangeTest() {
        ListUtils.clearRange(list, 2, 5);

        var newList = List.of(0, 1, 5, 6, 7, 8, 9, 10);

        Assertions.assertEquals(newList, list);
    }
}
