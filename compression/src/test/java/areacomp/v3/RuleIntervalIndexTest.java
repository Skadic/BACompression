package areacomp.v3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.TreeMap;

public class RuleIntervalIndexTest {

    private Ruleset ruleset;
    private RuleIntervalIndex index;
    private Rule topLevelRule;
    private int topLevelId;

    private static final int LENGTH = 16;
    private static final String STR = "abacabacabacabac";

    @BeforeEach
    public void setUp() throws Exception {
        ruleset = new Ruleset(STR);
        topLevelRule = ruleset.getTopLevelRule();
        topLevelId = topLevelRule.getId();
        index = new RuleIntervalIndex(topLevelId, LENGTH);
    }

    @Test
    public void testConstructor() {
        final var map = new TreeMap<Integer, RuleIntervalIndex.RuleInterval>();
        map.put(0, new RuleIntervalIndex.RuleInterval(topLevelId, 0, LENGTH - 1));

        Assertions.assertEquals(map, index.getIntervalMap(), "Maps not equal");
    }

    @Test
    public void testMarkSimple() {
        index.mark(1, 4, 8);

        final var map = new TreeMap<Integer, RuleIntervalIndex.RuleInterval>();
        map.put(0, new RuleIntervalIndex.RuleInterval(topLevelId, 0, 3));
        map.put(4, new RuleIntervalIndex.RuleInterval(1, 4, 8));
        map.put(9, new RuleIntervalIndex.RuleInterval(topLevelId, 9, 15));

        Assertions.assertEquals(map, index.getIntervalMap(), "Maps not equal");
        Assertions.assertEquals(
                index.rangeByStartIndex(0).flatMap(RuleIntervalIndex.RuleInterval::next),
                index.rangeByStartIndex(9),
                "Interval Parts incorrectly linked"
        );
    }

    @Test
    public void testMarkOverlapping() {
        index.mark(1, 4, 8);
        index.mark(2, 2, 10);

        final var map = new TreeMap<Integer, RuleIntervalIndex.RuleInterval>();
        map.put(0, new RuleIntervalIndex.RuleInterval(topLevelId, 0, 1));
        map.put(2, new RuleIntervalIndex.RuleInterval(2, 2, 3));
        map.put(4, new RuleIntervalIndex.RuleInterval(1, 4, 8));
        map.put(9, new RuleIntervalIndex.RuleInterval(2, 9, 10));
        map.put(11, new RuleIntervalIndex.RuleInterval(topLevelId, 11, 15));

        Assertions.assertEquals(map, index.getIntervalMap(), "Maps not equal");

        Assertions.assertEquals(
                index.rangeByStartIndex(0).flatMap(RuleIntervalIndex.RuleInterval::next),
                index.rangeByStartIndex(11),
                "Interval Parts incorrectly linked"
        );
    }

    @Test
    public void testIntervalPartLink() {
        index.mark(1, 2, 10);
        index.mark(2, 4, 8);

        final var map = new TreeMap<Integer, RuleIntervalIndex.RuleInterval>();
        map.put(0, new RuleIntervalIndex.RuleInterval(topLevelId, 0, 1));
        map.put(2, new RuleIntervalIndex.RuleInterval(1, 2, 3));
        map.put(4, new RuleIntervalIndex.RuleInterval(2, 4, 8));
        map.put(9, new RuleIntervalIndex.RuleInterval(1, 9, 10));
        map.put(11, new RuleIntervalIndex.RuleInterval(topLevelId, 11, 15));

        Assertions.assertEquals(map, index.getIntervalMap(), "Maps not equal");

        Assertions.assertEquals(
                index.rangeByStartIndex(0).flatMap(RuleIntervalIndex.RuleInterval::next),
                index.rangeByStartIndex(11),
                "Interval Parts incorrectly linked"
        );

        Assertions.assertEquals(
                index.rangeByStartIndex(2).flatMap(RuleIntervalIndex.RuleInterval::next),
                index.rangeByStartIndex(9),
                "Interval Parts incorrectly linked"
        );
    }

    @Test
    public void testAdjacentIntervalPartLink() {

        index.mark(1, 2, 4);
        index.mark(1, 5, 7);

        final var map = new TreeMap<Integer, RuleIntervalIndex.RuleInterval>();
        map.put(0, new RuleIntervalIndex.RuleInterval(topLevelId, 0, 1));
        map.put(2, new RuleIntervalIndex.RuleInterval(1, 2, 4));
        map.put(5, new RuleIntervalIndex.RuleInterval(1, 5, 7));
        map.put(8, new RuleIntervalIndex.RuleInterval(topLevelId, 8, 15));

        Assertions.assertEquals(map, index.getIntervalMap(), "Maps not equal");

        Assertions.assertEquals(
                index.rangeByStartIndex(0).flatMap(RuleIntervalIndex.RuleInterval::next),
                index.rangeByStartIndex(8),
                "Interval Parts incorrectly linked"
        );

        Assertions.assertEquals(
                index.rangeByStartIndex(2).flatMap(RuleIntervalIndex.RuleInterval::next),
                Optional.empty(),
                "Interval Parts linked even though they are separate"
        );

        Assertions.assertEquals(
                index.rangeByStartIndex(5).flatMap(RuleIntervalIndex.RuleInterval::prev),
                Optional.empty(),
                "Interval Parts linked even though they are separate"
        );
    }

    @Test
    public void testRange() {
        Assertions.assertEquals(0, index.range(0).start(), "Failed, when searched index is same as interval start");
        Assertions.assertEquals(0, index.range(5).start(), "Failed, when searched index is in the interval");
        Assertions.assertEquals(0, index.range(15).start(), "Failed, when searched index is same as interval end");
    }

    @Test
    public void testRangeSkipInterval() {
        index.mark(1, 4, 8);

        Assertions.assertEquals(0, index.range(2).start(), "Failed, when before inserted interval");
        Assertions.assertEquals(4, index.range(5).start(), "Failed, when in searched index is in the inserted interval");
        Assertions.assertEquals(0, index.range(15).start(), "Failed, when searched index is after the inserted interval");

        System.out.println(index.toString());
    }


}
