package compression.areacomp.v4;

import compression.utils.BucketPred;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

class RuleIntervalIndex {

    /**
     * The Predecessor structure, which contains all intervals that are substituted by some production rule, starting at a given index
     * This maps from the start index of an interval, to the deepest nested {@link RuleInterval} at the index.
     * The less deeply nested Intervals can be accessed using {@link RuleInterval#parent()}.
     */
    private final BucketPred<RuleInterval> intervalMap;

    private final int len;

    /**
     * Creates a new index structure
     * @param topLevelRuleId The id of the top-level rule
     * @param len The length of the input text
     */
    public RuleIntervalIndex(int topLevelRuleId, int len) {
        this.len = len;
        this.intervalMap = new BucketPred<>(len, 12);
        //this.intervalMap = new PredecessorNavigableMapAdapter<>(new TreeMap<>());
        //this.intervalMap = new XFastTrie<>();
        intervalMap.put(topLevelRuleId, new RuleInterval(topLevelRuleId, 0, len - 1, 0));
    }

    /**
     * Marks the area with the given rule id
     * @param ruleId The rule id
     * @param start inclusive
     * @param end inclusive
     */
    public void mark(int ruleId, int start, int end) {
        checkInterval(start, end);
        RuleInterval current = deepestNestedIntervalAtStartIndex(start);
        var interval = new RuleInterval(ruleId, start, end, -1);

        // If there was no interval at that index before
        if(current == null) {
            var parent = deepestIntervalContaining(start, end);
            interval.depth = parent.depth + 1;
            interval.insertParent(parent);
            interval.setFirstAtStartIndex(interval);
            intervalMap.put(start, interval);
            return;
        }

        // If this new interval is the new deepest nested interval
        // Add the new interval into its appropriate place in the list according to its depth
        if(current.contains(interval)) {
            interval.insertParent(current);
            interval.depth = current.depth + 1;
            interval.setFirstAtStartIndex(current.firstAtStartIndex());
            // Since current is now the new deepest interval, it replaces the previous deepest interval
            intervalMap.put(start, interval);
            return;
        }

        for(; true; current = current.parent()) {
            current.depth++;

            // If there are no more less-deeply nested intervals at this start index, break
            if(!current.hasParent() || current.parent().start() != start || interval.parent().contains(interval)) {
                break;
            }
        }

        current.insertParent(interval);
        interval.setFirstAtStartIndex(current.hasParent() ? current.parent().firstAtStartIndex() : interval);
    }

    /**
     * Gets the rule id of the deepest nested interval that contains the given index
     * @param index The index to search for
     * @return The rule id of the deepest interval that contains the given index if it exists. -1 otherwise
     */
    public int deepestRuleIdAt(int index) {
        checkIndex(index);
        RuleInterval ruleInterval = deepestIntervalContaining(index, index);
        return ruleInterval != null ? ruleInterval.ruleId : -1;
    }

    /**
     * Gets the start index of the deepest nested interval that contains the given index
     * @param index The index to search for
     * @return The start id of the deepest interval that contains the given index if it exists. -1 otherwise
     */
    public int intervalStartIndex(int index) {
        checkIndex(index);
        RuleInterval ruleInterval = deepestIntervalContaining(index, index);
        return ruleInterval != null ? ruleInterval.start : -1;
    }

    /**
     * Get the deepest nested interval that contains the given interval
     * @param from The inclusive start index of the interval to check for
     * @param to The inclusive end index of the interval to check for
     * @return Return the deepest nested interval that contains the interval [from, to] if there is such an interval. null otherwise
     */
    public RuleInterval deepestIntervalContaining(int from, int to) {
        checkInterval(from, to);
        RuleInterval current = deepestFloorInterval(from);
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
    public RuleInterval deepestIntervalContainingIndex(int index) {
        //checkIndex(index);
        return deepestIntervalContaining(index, index);
    }


    /**
     * Gets the most deeply nested interval that starts at this index
     * @param index The index to search for
     * @return
     */
    public RuleInterval deepestNestedIntervalAtStartIndex(int index) {
        checkIndex(index);
        return intervalMap.get(index);
    }

    /**
     * Returns the deepest nested interval at the greatest index smaller or equal to the given index
     * @param index
     * @return
     */
    private RuleInterval deepestFloorInterval(int index) {
        //checkIndex(index);
        final var entry = intervalMap.floorEntry(index);
        return entry.value();
    }

    public int length() {
        return len;
    }

    @Override
    public String toString() {
        return intervalMap.values().stream()
                .map(deepestInterval -> {
                    final var sj = new StringJoiner(", ", "%d: [".formatted(deepestInterval.start()), "]");
                    deepestInterval.forEach(interval -> sj.add("%d: %d".formatted(interval.ruleId, interval.end)));
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

    static class RuleInterval implements Iterable<RuleInterval> {

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

        public void insertParent(RuleInterval newParent) {
            if(hasParent()) {
                newParent.parent = this.parent;
            }
            this.parent = newParent;
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

        /**
         * Retrieves this interval's parent Interval
         * @return The parent interval, or null if there is no parent
         *
         * @see #parent
         */
        public RuleInterval parent() {
            return parent;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "(", ")").add("R" + ruleId).add(String.valueOf(start)).add(String.valueOf(end)).add(String.valueOf(depth)).toString();
        }

        /**
         * Returns true, if the other Interval is contained in the bounds of this one
         * @return true, if the other Interval is contained in the bounds of this one, false if other is null or the other interval is not contained in this one
         */
        public boolean contains(RuleInterval other) {
            if(other == null) return false;
            return this.start <= other.start && other.end <= this.end;
        }

        @Override
        public Iterator<RuleInterval> iterator() {
            return new Iterator<>() {
                RuleInterval current = RuleInterval.this;
                final int start = RuleInterval.this.start;

                @Override
                public boolean hasNext() {
                    return current != null && current.start == start;
                }

                @Override
                public RuleInterval next() {
                    if(!hasNext()) throw new NoSuchElementException();

                    final var temp = current;
                    current = current.parent();
                    return temp;
                }
            };
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
