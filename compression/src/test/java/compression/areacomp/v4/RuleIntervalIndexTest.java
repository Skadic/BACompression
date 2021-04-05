package compression.areacomp.v4;

import compression.areacomp.v4.RuleIntervalIndex.RuleInterval;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RuleIntervalIndexTest {

    private RuleIntervalIndex index;
    private static final String STR = "abacabacabacabac";
    private static final int LENGTH = STR.length();

    @BeforeEach
    public void setUp() {
        index = new RuleIntervalIndex(0, LENGTH);
    }

    @Test
    public void testMark() {
        Assertions.assertEquals(List.of(new RuleInterval(0, 0, LENGTH - 1, 0)), index.intervalsAtStartIndex(0));
        index.mark(1, 0, 10);
        Assertions.assertEquals(List.of(
                new RuleInterval(0, 0, LENGTH - 1, 0),
                new RuleInterval(1, 0, 10, 1)
        ), index.intervalsAtStartIndex(0));

        index.mark(2, 3, 5);

        Assertions.assertEquals(List.of(
                new RuleInterval(0, 0, LENGTH - 1, 0),
                new RuleInterval(1, 0, 10, 1)
        ), index.intervalsAtStartIndex(0));

        Assertions.assertEquals(List.of(
                new RuleInterval(2, 3, 5, 2)
        ), index.intervalsAtStartIndex(3));

    }

    @Test
    public void testDeepestContainingInterval() {
        index.mark(1, 0, 10);
        index.mark(2, 3, 8);
        index.mark(3, 5, 8);
        index.mark(4, 6, 7);

        Assertions.assertEquals(new RuleInterval(0, 0, LENGTH - 1, 0), index.deepestContainingInterval(5, 11));
        Assertions.assertEquals(new RuleInterval(0, 0, LENGTH - 1, 0), index.deepestContainingInterval(8, 15));
        Assertions.assertEquals(new RuleInterval(1, 0, 10, 1), index.deepestContainingInterval(2, 4));
        Assertions.assertEquals(new RuleInterval(1, 0, 10, 1), index.deepestContainingInterval(1, 7));
        Assertions.assertEquals(new RuleInterval(2, 3, 8, 2), index.deepestContainingInterval(4, 7));
        Assertions.assertEquals(new RuleInterval(2, 3, 8, 2), index.deepestContainingInterval(3, 8));
        Assertions.assertEquals(new RuleInterval(3, 5, 8, 3), index.deepestContainingInterval(5, 7));
        Assertions.assertEquals(new RuleInterval(3, 5, 8, 3), index.deepestContainingInterval(7, 8));
        Assertions.assertEquals(new RuleInterval(4, 6, 7, 4), index.deepestContainingInterval(6, 7));
        Assertions.assertEquals(new RuleInterval(4, 6, 7, 4), index.deepestContainingInterval(6, 6));
        Assertions.assertEquals(new RuleInterval(4, 6, 7, 4), index.deepestContainingInterval(7, 7));
    }
}
