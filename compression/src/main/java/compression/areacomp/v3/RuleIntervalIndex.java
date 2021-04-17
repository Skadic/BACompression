package compression.areacomp.v3;

import compression.utils.Benchmark;
import compression.utils.BucketPred;
import compression.utils.IntPredecessor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class RuleIntervalIndex {

    private final IntPredecessor<RuleInterval> intervalMap;

    private final int len;

    public RuleIntervalIndex(int topLevelRuleId, int len) {
        this.len = len;
        this.intervalMap = new BucketPred<>(len, 10);
        //this.intervalMap = new PredecessorNavigableMapAdapter<>(new TreeMap<>());
        //this.intervalMap = new XFastTrie<>();
        intervalMap.put(topLevelRuleId, new RuleInterval(topLevelRuleId, 0, len - 1));
    }

    /**
     * Marks the area with the given rule id
     * @param ruleId The rule id
     * @param start inclusive
     * @param end inclusive
     */
    public void mark(int ruleId, int start, int end) {
        checkIndex(start);
        checkIndex(end);

        Benchmark.startTimer("Index", "mark intervals");

        RuleInterval startInterval = intervalMap.floorEntry(start).value();
        RuleInterval endInterval;
        if (startInterval.end() < end) { // In this case, the interval was already removed when calling removeFloor(start)
            endInterval = intervalMap.floorEntry(end).value();
        } else {
            endInterval = startInterval;
        }

        Benchmark.stopTimer("Index", "mark intervals");

        Benchmark.startTimer("Index", "mark add borders");
        addBorderIntervals(start, end, startInterval, endInterval);
        Benchmark.stopTimer("Index", "mark add borders");

        Benchmark.startTimer("Index", "mark replace intervals");
        replaceIntervals(ruleId, start, end, startInterval);
        Benchmark.stopTimer("Index", "mark replace intervals");
    }

    private void addBorderIntervals(int start, int end, RuleInterval startInterval, RuleInterval endInterval) {
        // The cut-off part of the surrounding interval before the new interval
        RuleInterval before = new RuleInterval(startInterval.ruleId(), startInterval.start, start - 1);
        // The cut-off part of the surrounding interval after the new interval
        RuleInterval after = new RuleInterval(endInterval.ruleId(), end + 1, endInterval.end);
        // Placeholder intervals taking the space that was cut out of the surrounding interval.
        // This is replaced with the actual contents.
        RuleInterval first = new RuleInterval(-1, start, startInterval.end);
        RuleInterval last = new RuleInterval(-1, endInterval.start, end);
        before.setTotalStart(startInterval.totalStart);
        after.setTotalStart(endInterval.totalStart);
        first.setTotalStart(-1);
        last.setTotalStart(-1);

        Benchmark.startTimer("Index", "add border put");
        intervalMap.put(first.start, first);
        intervalMap.put(last.start, last);
        Benchmark.stopTimer("Index", "add border put");

        // If the to-be-marked interval starts after the interval in which its start index lies, then insert a new Interval
        // which spans from the original start, to before the new start
        if(startInterval.start < start) {
            // Connect the previous part of the interval to the new start interval
            RuleInterval.connect(startInterval.prevSlice, before);
            intervalMap.put(before.start, before);
        } else {
            // If there is no new start interval, then connect the previous part of the interval to the part after the new interval
            RuleInterval.connect(startInterval.prevSlice, after);
        }

        // Similar to the start interval
        if(end < endInterval.end) {
            RuleInterval.connect(after, endInterval.nextSlice);
            intervalMap.put(after.start, after);
        } else {
            RuleInterval.connect(before, endInterval.nextSlice);
        }


        // If the new interval is completely enclosed by the old interval, link the before and after
        if(startInterval.start < start && end < endInterval.end) {
            RuleInterval.connect(before, after);
        }
    }

    /**
     *  A temporary list, to prevent a ConcurrentModificationException to be thrown when modifying the map, while the
     *  iterator exists. Only to be used in {@link #replaceIntervals(int, int, int, RuleInterval)}
     */
    private final Deque<RuleInterval> TO_INSERT = new ArrayDeque<>();

    private void replaceIntervals(int ruleId, int start, int end, RuleInterval startInterval) {

        // Add all intervals that are between start and end
        var startIndex = -1;
        var endIndex = -1;

        RuleInterval firstInterval = null;
        RuleInterval lastInterval = null;



        for(
                Iterator<RuleInterval> iterator = intervalMap.valueRangeIterator(startInterval.start, true, end, true);
                iterator.hasNext();
        ){
            RuleInterval interval = iterator.next();
            // If the entire interval's start is before the start of the new interval, then it can't be nested deeper than this one
            if(interval.totalStart <= start) {
                // If no start index is set, set it
                if(startIndex == -1) {
                    startIndex = Math.max(interval.start, start);
                }
                // Update the end index
                endIndex = Math.min(interval.end, end);
            }

            if((interval.totalStart > start || !iterator.hasNext()) && startIndex > -1 && endIndex > -1){

                // Create a new interval and insert it
                RuleInterval ruleInterval = new RuleInterval(ruleId, startIndex, endIndex);
                ruleInterval.setTotalStart(start);
                if(firstInterval == null) {
                    firstInterval = ruleInterval;
                }
                if(lastInterval != null){
                    lastInterval.append(ruleInterval);
                }
                TO_INSERT.add(ruleInterval);
                lastInterval = ruleInterval;
                startIndex = -1;
                endIndex = -1;
            }
        }

        while (!TO_INSERT.isEmpty()) {
            var interval = TO_INSERT.pop();
            intervalMap.put(interval.start, interval);
        }
    }

    public int deepestRuleAt(int index) {
        final RuleInterval value = intervalMap.floorEntry(index).value();
        return value.ruleId();
    }

    public RuleInterval startInterval(int index) {
        checkIndex(index);
        Benchmark.startTimer("Index", "startInterval");
        final RuleInterval ruleInterval = intervalMap.get(intervalMap.floorEntry(index).value().totalStart);
        Benchmark.stopTimer("Index", "startInterval");

        return ruleInterval;
    }

    public int totalStartIndex(int index) {
        checkIndex(index);
        Benchmark.startTimer("Index", "startInterval");
        final int ruleInterval = intervalMap.floorEntry(index).value().totalStart;
        Benchmark.stopTimer("Index", "startInterval");

        return ruleInterval;
    }



    public RuleInterval intervalAt(int index) {
        Benchmark.startTimer("Index", "intervalAt");
        final RuleInterval value = intervalMap.floorEntry(index).value();
        Benchmark.stopTimer("Index", "intervalAt");
        return value;
    }

    public Optional<RuleInterval> rangeByStartIndex(int index) {
        checkIndex(index);
        return Optional.ofNullable(intervalMap.getOrDefault(index, null));
    }

    public int length() {
        return len;
    }

    public IntPredecessor<RuleInterval> getIntervalMap() {
        return intervalMap;
    }

    @Override
    public String toString() {
        return intervalMap.values().stream()
                .flatMapToInt(ruleInterval -> IntStream.iterate(ruleInterval.ruleId(), i -> i).limit(ruleInterval.length()))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));
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
        private int ruleId;

        /**
         * If this Interval is just part of a greater interval, then this field holds the start index of the first subinterval
         * of the greater interval
         */
        private int totalStart;

        /**
         * Inclusive start index of the interval
         */
        private int start;

        /**
         * Inclusive end interval of the interval
         */
        private int end;

        private RuleInterval nextSlice;
        private RuleInterval prevSlice;

        public RuleInterval(int ruleId, int start, int end) {
            this.totalStart = start;
            this.start = start;
            this.end = end;
            this.ruleId = ruleId;

            this.nextSlice = null;
            this.prevSlice = null;
        }

        public static void connect(RuleInterval first, RuleInterval second) {
            if(first == null && second == null) return;

            if(first == null) {
                second.disconnectPrev();
            } else if (second == null) {
                first.disconnectNext();
            } else {
                first.append(second);
            }
        }

        public void setTotalStart(int totalStart) {
            this.totalStart = totalStart;
        }

        public void append(RuleInterval other) {
            if (other == null) {
                disconnectNext();
            }
            nextSlice = other;
            other.prevSlice = this;
            nextSlice.totalStart = this.totalStart;
        }

        public void disconnectNext() {
            if(nextSlice == null) return;
            nextSlice.prevSlice = null;
            nextSlice = null;
        }

        public void disconnectPrev() {
            if(prevSlice == null) return;
            prevSlice.nextSlice = null;
            prevSlice = null;
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

        public Optional<RuleInterval> prev() {
            return Optional.ofNullable(prevSlice);
        }

        public Optional<RuleInterval> next() {
            return Optional.ofNullable(nextSlice);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "(", ")").add("R" + ruleId).add(String.valueOf(start)).add(String.valueOf(end)).toString();
        }

        public int totalStart() {
            return totalStart;
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
    }
}
