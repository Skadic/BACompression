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
        this.intervalMap = new BucketPred<>(len, 16);
        //this.intervalMap = new PredecessorNavigableMapAdapter<>(new TreeMap<>());
        //this.intervalMap = new XFastTrie<>();
        final var interval = new RuleInterval(topLevelRuleId, 0, len - 1);
        interval.setFirstAtStartIndex(interval);
        intervalMap.put(topLevelRuleId, interval);

    }

    /**
     * Marks the area with the given rule id
     * @param ruleId The rule id
     * @param start inclusive
     * @param end inclusive
     */
    public void mark(int ruleId, int start, int end) {
        checkInterval(start, end);
        RuleInterval current = intervalAtStartIndex(start);
        var interval = new RuleInterval(ruleId, start, end);

        // If there was no interval at that index before
        if(current == null) {
            var parent = intervalContaining(start, end);
            //interval.depth = parent.depth + 1;
            interval.insertParent(parent);
            interval.setFirstAtStartIndex(interval);
            intervalMap.put(start, interval);
            // If this new interval is the new deepest nested interval
            // Add the new interval into its appropriate place in the list according to its depth
        } else if(current.contains(interval)) {
            //interval.depth = current.depth + 1;
            interval.insertParent(current);
            interval.setFirstAtStartIndex(current.firstAtStartIndex());
            // Since current is now the new deepest interval, it replaces the previous deepest interval
            intervalMap.put(start, interval);
        } else {
            // Whether the new Interval will be the least deeply nested at this start index
            final boolean isNewFirst = interval.contains(current.firstAtStartIndex());
            while(true) {
                //current.depth++;
                if(isNewFirst) current.setFirstAtStartIndex(interval);
                // If there are no more less-deeply nested intervals at this start index, break
                if(!current.hasParent() || current.parent().start() != start || current.parent().contains(interval)) {
                    break;
                }

                current = current.parent();
            }

            //interval.depth = current.depth + 1;
            current.insertParent(interval);
            interval.setFirstAtStartIndex(interval.hasParent() && interval.start() == interval.parent().start() ? interval.parent().firstAtStartIndex() : interval);
        }

        intervalMap.valueRangeIterator(start, false, end, true).forEachRemaining(inter -> {
            var first  = inter.firstAtStartIndex();
            if(interval.contains(first) && first.parent() == interval.parent()) {
                first.insertParent(interval);
            }
        });
    }

    /**
     * Get the deepest nested interval that contains the given interval
     * @param from The inclusive start index of the interval to check for
     * @param to The inclusive end index of the interval to check for
     * @return Return the deepest nested interval that contains the interval [from, to] if there is such an interval. null otherwise
     */
    public RuleInterval intervalContaining(int from, int to) {
        checkInterval(from, to);
        RuleInterval current = floorInterval(from);
        //int i = 0;
        while (current != null) {
            // If an interval that contains index has been found, return
            if(current.start <= from && to <= current.end) {
                //if(i >= 20) System.out.println("Iterations: " + i);
                return current;
            }

            if(current.firstAtStartIndex().start() <= from && to <= current.firstAtStartIndex().end()) {
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
    public RuleInterval intervalContaining(int index) {
        //checkIndex(index);
        return intervalContaining(index, index);
    }

    /**
     * Gets the most deeply nested interval that starts at this index
     * @param index The index to search for
     * @return The interval if such exists, null otherwise
     */
    public RuleInterval intervalAtStartIndex(int index) {
        checkIndex(index);
        return intervalMap.get(index);
    }

    /**
     * Returns the deepest nested interval at the greatest index smaller or equal to the given index
     * @param index
     * @return
     */
    private RuleInterval floorInterval(int index) {
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
                    deepestInterval.firstAtStartIndex().deeperIterator().forEachRemaining(interval -> sj.add("%d: %d".formatted(interval.ruleId, interval.end)));
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

    static class RuleInterval {

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
         * The next most deepest Interval that also starts at this start index
         */
        private RuleInterval nextAtStartIndex;

        /**
         * Inclusive start index of the interval
         */
        private final int start;

        /**
         * Inclusive end interval of the interval
         */
        private final int end;

        public RuleInterval(int ruleId, int start, int end) {
            this.start = start;
            this.end = end;
            this.ruleId = ruleId;
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
                if(parent.start == newParent.start) {
                    this.parent.nextAtStartIndex = newParent;
                    newParent.firstAtStartIndex = this.parent.firstAtStartIndex;
                }
            }
            this.parent = newParent;
            if(newParent.start == this.start) {
                newParent.nextAtStartIndex = this;
                this.firstAtStartIndex = newParent.firstAtStartIndex;
            }
        }

        public void setFirstAtStartIndex(RuleInterval firstAtStartIndex) {
            this.firstAtStartIndex = firstAtStartIndex;
        }

        /**
         * Returns the least deeply nested interval that starts at the same start index
         * @return The interval
         */
        public RuleInterval firstAtStartIndex() {
            return this.firstAtStartIndex;
        }

        public boolean hasParent() {
            return parent != null;
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
            return new StringJoiner(", ", "(", ")").add("R" + ruleId).add(String.valueOf(start)).add(String.valueOf(end)).toString();
        }

        /**
         * Returns true, if the other Interval is contained in the bounds of this one
         * @return true, if the other Interval is contained in the bounds of this one, false if other is null or the other interval is not contained in this one
         */
        public boolean contains(RuleInterval other) {
            if(other == null) return false;
            return this.start <= other.start && other.end <= this.end;
        }

        /**
         * Returns an iterator that iterates through all Intervals that start at this index
         * and that are more deeply nested than this one, in order of their nesting.
         * The iterator's first element is the one on which the method was called.
         * @return The iterator
         */
        public Iterator<RuleInterval> deeperIterator() {
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
                    current = current.nextAtStartIndex();
                    return temp;
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RuleInterval that = (RuleInterval) o;
            return start == that.start && end == that.end && ruleId == that.ruleId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleId, start, end);
        }

        public RuleInterval nextAtStartIndex() {
            return nextAtStartIndex;
        }
    }
}
