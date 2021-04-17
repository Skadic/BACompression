package compression.areacomp.v4;

import compression.utils.BucketPred;
import compression.utils.IntPredecessor;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class RuleIntervalIndex {

    private static final Supplier<List<RuleInterval>> LIST_SUPPLIER = ArrayList::new;

    private final IntPredecessor<List<RuleInterval>> intervalMap;

    private final int len;

    public RuleIntervalIndex(int topLevelRuleId, int len) {
        this.len = len;
        this.intervalMap = new BucketPred<>(len, 12);
        //this.intervalMap = new PredecessorNavigableMapAdapter<>(new TreeMap<>());
        //this.intervalMap = new XFastTrie<>();
        List<RuleInterval> list = LIST_SUPPLIER.get();
        list.add(new RuleInterval(topLevelRuleId, 0, len - 1, 0));
        intervalMap.put(topLevelRuleId, list);
    }

    /**
     * Marks the area with the given rule id
     * @param ruleId The rule id
     * @param start inclusive
     * @param end inclusive
     */
    public void mark(int ruleId, int start, int end) {
        checkInterval(start, end);

        var parent = deepestContainingInterval(start, end);
        var list = intervalsAtStartIndexModifiable(start);

        if(list == null) {
            list = LIST_SUPPLIER.get();
            intervalMap.put(start, list);
        }

        // This interval is 1 deeper than its parent
        int intervalDepth = parent.depth + 1;
        var interval = new RuleInterval(ruleId, start, end, intervalDepth);


        // The index at which the new interval should go in the list
        int depthIndex = findDepthIndex(list, intervalDepth);

        list.add(depthIndex, interval);

        interval.setParent(parent);
        interval.setFirstAtStartIndex(list.get(0));

        // Start at the index after the newly added interval
        depthIndex++;
        if(depthIndex < list.size()) {
            list.get(depthIndex).setParent(interval);
        }
        for(; depthIndex < list.size(); depthIndex++) {
            list.get(depthIndex).depth++;
        }

    }

    /**
     * Find the index at which an interval with the given depth must fit in this list.
     * Since the list contains depth values in ascending order, this is just a binary search.
     *
     * @param intervals The list of rule intervals in which to search for the correct place
     * @param depth The depth of a new interval that is to be inserted into the list
     * @return The index at which the new interval should be inserted
     */
    private static int findDepthIndex(List<RuleInterval> intervals, int depth) {
        if(intervals.isEmpty() || intervals.get(intervals.size() - 1).depth < depth) return intervals.size();

        int lo = 0;
        int hi = intervals.size() - 1;

        while(lo <= hi) {
            final int mid = (lo + hi) / 2;
            final int midDepth = intervals.get(mid).depth;
            if(midDepth == depth) {
                return mid;
            } else if (depth < midDepth) {
                hi = mid;
            } else {
                lo = mid;
            }
        }

        return -1;
    }

    /**
     * Gets the rule id of the deepest nested interval that contains the given index
     * @param index The index to search for
     * @return The rule id of the deepest interval that contains the given index if it exists. -1 otherwise
     */
    public int deepestRuleIdAt(int index) {
        checkIndex(index);
        RuleInterval ruleInterval = deepestContainingInterval(index, index);
        return ruleInterval != null ? ruleInterval.ruleId : -1;
    }

    /**
     * Gets the start index of the deepest nested interval that contains the given index
     * @param index The index to search for
     * @return The start id of the deepest interval that contains the given index if it exists. -1 otherwise
     */
    public int intervalStartIndex(int index) {
        checkIndex(index);
        RuleInterval ruleInterval = deepestContainingInterval(index, index);
        return ruleInterval != null ? ruleInterval.start : -1;
    }

    /**
     * Get the deepest nested interval that contains the given interval
     * @param from The inclusive start index of the interval to check for
     * @param to The inclusive end index of the interval to check for
     * @return Return the deepest nested interval that contains the interval [from, to] if there is such an interval. null otherwise
     */
    public RuleInterval deepestContainingInterval(int from, int to) {
        checkInterval(from, to);
        // Should actually be floorIntervalsModifiable here
        List<RuleInterval> currentList = intervalMap.floorEntry(from).value();
        RuleInterval current = currentList.get(currentList.size() - 1);
        //int i = 0;
        while (current != null) {
            // If an interval that contains index has been found, return
            if(to <= current.end) {
                //if(i >= 20) System.out.println("Iterations: " + i);
                return current;
            }

            if(to <= current.firstAtStartIndex().end()) {
                current = current.parent();
            } else {
                current = current.firstAtStartIndex();
                if(current.hasParent()) {
                    current = current.parent();
                }
            }
            //i++;
        }
        //if(i >= 20) System.out.println("Iterations: " + i);
        return null;
    }

    /**
     * Returns the deepest nested interval that contains the given index
     * @param index The index to search for
     * @return The deepest nested interval which contains the index
     */
    public RuleInterval deepestIntervalAt(int index) {
        //checkIndex(index);
        return deepestContainingInterval(index, index);
    }


    private List<RuleInterval> intervalsAtStartIndexModifiable(int index) {
        checkIndex(index);
        return intervalMap.get(index);
    }

    /**
     * Get the unmodifiable list of intervals that start at the given index.
     * @param index The start index of the intervals to search for
     * @return The unmodifiable list of intervals that start at the given index if there are any. null otherwise
     */
    public List<RuleInterval> intervalsAtStartIndex(int index) {
        List<RuleInterval> list = intervalsAtStartIndexModifiable(index);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    private List<RuleInterval> floorIntervalsModifiable(int index) {
        //checkIndex(index);
        final var entry = intervalMap.floorEntry(index);
        return entry.value();
    }

    public List<RuleInterval> floorIntervals(int index) {
        List<RuleInterval> list = floorIntervalsModifiable(index);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public int length() {
        return len;
    }

    @Override
    public String toString() {
        return intervalMap.values().stream()
                .map(list -> {
                    final var sj = new StringJoiner(", ", "%d: [".formatted(list.get(0).start), "]");
                    list.forEach(interval -> sj.add("%d: %d".formatted(interval.ruleId, interval.end)));
                    return sj.toString();
                })
                .collect(Collectors.joining(", "));
    }


    /**
     * Check whether [from, to] make a legal interval that is in range of this data structure. Throws an Exception, if this
     * range is illegal
     * @param from The inclusive start index of the interval
     * @param to The inclusive to index of the interval
     */
    private void checkInterval(int from, int to) {
        if(from < 0) throw new IndexOutOfBoundsException("from = " + from);
        if(to >= len) throw new IndexOutOfBoundsException("to = " + to);
        if(to < from) throw new IllegalArgumentException("to(%d) < from(%d)".formatted(to, from));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException("Index '" + index + "' out of range for length " + length());
        }
    }

    static class RuleInterval  {

        /**
         * The id of the rule this interval is factorized by
         */
        private final int ruleId;

        /**
         * The Interval in which this one is nested in
         */
        private RuleInterval parent;

        /**
         * The least nested rule that also starts at this index
         */
        private RuleInterval firstAtStartIndex;

        /**
         * The nesting depth of this interval
         */
        private int depth;

        /**
         * Inclusive start index of the interval
         */
        private final int start;

        /**
         * Inclusive end interval of the interval
         */
        private final int end;

        public RuleInterval(int ruleId, int start, int end, int depth) {
            this.start = start;
            this.end = end;
            this.ruleId = ruleId;
            this.depth = depth;
            this.parent = null;
            this.firstAtStartIndex = null;
        }

        public int ruleId() {
            return ruleId;
        }

        public int start() {
            return start;
        }

        public int end() {
            return end;
        }

        public int length() {
            return end - start;
        }


        public void setFirstAtStartIndex(RuleInterval firstAtStartIndex) {
            this.firstAtStartIndex = firstAtStartIndex;
        }

        public RuleInterval firstAtStartIndex() {
            return this.firstAtStartIndex;
        }

        public boolean hasParent() {
            return parent != null;
        }

        public void setParent(RuleInterval parent) {
            this.parent = parent;
        }

        public RuleInterval parent() {
            return parent;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "(", ")").add("R" + ruleId).add(String.valueOf(start)).add(String.valueOf(end)).add(String.valueOf(depth)).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RuleInterval that = (RuleInterval) o;
            return start == that.start && end == that.end && ruleId == that.ruleId && depth == that.depth;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleId, start, end, depth);
        }
    }
}
