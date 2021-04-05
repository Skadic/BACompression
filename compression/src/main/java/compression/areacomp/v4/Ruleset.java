package compression.areacomp.v4;

import compression.areacomp.AreaFunction;
import compression.areacomp.v4.RuleIntervalIndex.RuleInterval;
import compression.unified.UnifiedNonTerminal;
import compression.unified.UnifiedRuleset;
import compression.unified.UnifiedTerminal;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedSymbol;
import compression.utils.AugmentedString;
import compression.utils.Benchmark;
import org.apache.commons.collections4.list.TreeList;

import java.util.*;

@SuppressWarnings("Duplicates")
class Ruleset implements ToUnifiedRuleset {

    // TODO If a grammar exists like
    //  A -> BBcBBc
    //  B -> ab
    //  It would not replace BBc with a new rule, since the rule index looks like this:
    //  B[0,1], B[2,3], A[4,4], B[5,6], B[7,8], A[9,9]
    //  Therefore, a request for the deepest rule at 0 and 4 respectively, would yield B and A respectively
    //  The algorithm would forbid replacing this, though this would be valid
    //  There likely needs to be a (most likely larger) change to RuleIntervalIndex to support multiple values at the same index
    //  That might incur a runtime penalty, but also could make markRange faster, since it does not need to iterate through intervals to override
    //  and link slices of the same interval. Also it might cause a memory penalty.

    public static final String ALGORITHM_NAME = AreaCompV4.class.getSimpleName();

    /**
     * The string for which this ruleset is built
     */
    private final String underlying;

    private final RuleIntervalIndex intervalIndex;

    private final Map<Integer, List<RuleIntervalStarts>> ruleRangeStarts;

    private record RuleIntervalStarts(int id, int start, int end) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RuleIntervalStarts that = (RuleIntervalStarts) o;
            return id == that.id && start == that.start && end == that.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, start, end);
        }
    }

    /**
     * Amount of rules in the set. This is used for getting new rule IDs in {@link #nextRuleID()}
     */
    private int numRules;

    /**
     * Creates a new ruleset to compress a string with.
     *
     * @param s The string for which the ruleset should be created
     */
    public Ruleset(String s) {
        Benchmark.startTimer(ALGORITHM_NAME, "construction");
        intervalIndex = new RuleIntervalIndex(0, s.length());
        ruleRangeStarts = new HashMap<>();
        ruleRangeStarts.put(0, new TreeList<>());
        ruleRangeStarts.get(0).add(new RuleIntervalStarts(0, 0, s.length() - 1));
        underlying = s;
        Benchmark.stopTimer(ALGORITHM_NAME, "construction");
        numRules = 1;
    }

    /**
     * Compresses the ruleset using an area function.
     * The area function determines how the intervals in the priority queue are prioritised.
     *
     * @param fun The area function used to prioritise intervals in the lcp array
     */
    public void compress(AreaFunction fun) {
        Benchmark.startTimer(ALGORITHM_NAME, "total time");

        Benchmark.startTimer(ALGORITHM_NAME, "suffix array");
        final var augS = new AugmentedString(underlying);
        Benchmark.stopTimer(ALGORITHM_NAME, "suffix array");

        Benchmark.startTimer(ALGORITHM_NAME, "queue");
        // Create a Priority Queue which holds possible intervals in the LCP array
        // The priority value is calculated through the given area function.
        // This function should return a high value for promising intervals in the LCP array.
        var queue = new PriorityQueue<AreaFunction.AreaData>(
                Comparator.comparingInt(
                        areaData -> -areaData.area
                )
        );

        for (var interval : augS.getLCPIntervals(2)) {
            final var data = fun.area(augS, interval.start() + 1, interval.end());
            if(data.area > 0)
                queue.add(data);
        }

        Benchmark.stopTimer(ALGORITHM_NAME, "queue");

        while (!queue.isEmpty()) {

            Benchmark.startTimer(ALGORITHM_NAME, "positions");
            // Poll the best interval
            var areaData = queue.remove();

            // The positions at which the pattern can be found
            int[] positions = areaData.viablePositions;

            // Get the length of the longest common prefix in this range of the lcp array
            // This will be the length of the pattern that is to be replaced.
            int len = areaData.len;

            // This means there is no repeated subsequence of 2 or more characters. In this case, abort
            if (len <= 1) {
                break;
            }

            Arrays.sort(positions);

            //positions = nonOverlapping(positions, len);
            Benchmark.startTimer(ALGORITHM_NAME, "in boundary");
            positions = inBoundary(positions, len);
            Benchmark.stopTimer(ALGORITHM_NAME, "in boundary");

            Benchmark.startTimer(ALGORITHM_NAME, "multiple occurrences");
            final var multipleOccurrences = differingOccurences(positions);
            Benchmark.stopTimer(ALGORITHM_NAME, "multiple occurrences");

            Benchmark.stopTimer(ALGORITHM_NAME, "positions");

            if(positions.length <= 1 || !multipleOccurrences) {
                continue;
            }

            Benchmark.startTimer(ALGORITHM_NAME, "factorize");
            factorize(len, positions);
            Benchmark.stopTimer(ALGORITHM_NAME, "factorize");
        }
        Benchmark.stopTimer(ALGORITHM_NAME, "total time");
    }

    /**
     * Factorizes a repeated sequence. All occurrences of the pattern in the grammar will be replaced with a new rule
     * This method makes the following assumptions
     * All occurences are non overlapping.
     * The positions are sorted in ascending order.
     * @param len The length of the subsequence to replace in terminal characters
     * @param positions The start positions of the occurences in the original string
     */
    public void factorize(int len, int... positions) {
        // If the occurence is less than one or
        if (len <= 1 || positions.length < 2) return;

        final int nextId = nextRuleID();

        // Iterate through the positions and substitute each occurrence.
        for (int position : positions) {
            Benchmark.startTimer(ALGORITHM_NAME, "markRange");
            markRange(nextId, position, position + len - 1);
            Benchmark.stopTimer(ALGORITHM_NAME, "markRange");
        }
    }

    /**
     * Determines, whether there are multiple Occurrences of the same pattern in distinct contexts regarding already existing rules
     * For example, the string "aaaaaa" and the grammar
     * R0 -> R1 R1
     * R1 -> aaa
     * The algorithm might try to replace the pattern "aa" at positions 0, 2 and 4, since it only computed the Suffix- and LCP-Array in the beginning.
     * Since in the algorithm, pattern occurrences, that cross the boundaries of pre-existing rules are removed before this algorithm is called,
     * the occurrence at position 2 is removed. This is the case, because it would cross the boundary from the first R1 range into the second R1 range,
     * if you look at the right side of R0.
     * But even so, factoring out "aa" at index 0 and 4 is not possible, since the pattern would appear once as the first "aa" in R1
     * and once as the second "aa" in R1. This method detects, if the given positions cannot be factored out.
     *
     * @param positions The positions to check
     * @return true, if there are positions that can be factored out, false otherwise
     */
    private boolean differingOccurences(int[] positions) {
        if(positions.length == 0) return false;

        int firstRuleId = -1;

        // Map from rule area start -> rule id -> occurrence count
        var set = new HashSet<Integer>();

        // Count how often this pattern appears at specific indices inside the right side of a rule
        // E.g. if the String is "abacdabac" and only the Rule "R1 -> abac" exists so far (which implies the structure "R1 d R1")
        // then the pattern "c" appears twice in the string in rule 1 at index 3
        // The corresponding map entry would then be (1, (3, 2))
        for (var pos : positions) {
            final var ruleInterval = intervalIndex.deepestIntervalAt(pos);
            final var ruleId = ruleInterval.ruleId();
            final var startIndex = ruleInterval.start();

            if(firstRuleId == -1){
                firstRuleId = ruleId;
            } else if(ruleId != firstRuleId || set.contains(startIndex)) {
                // This interval (or at least one that starts at the same index) already has an interval.
                // This new one must be distinct from that other one. If it's the very same interval, that is obvious.
                // If this is a different interval from before, it can't be the same rule id, since then it would have a later start id
                // Therefore the occurrences must be distinct also
                return true;
            }
            set.add(startIndex);
        }


        return false;
    }


    private static final List<Integer> IN_BOUNDARY_LIST = new ArrayList<>();

    /**
     * Filters out the occurrences of the pattern with length len and at the given positions, which start in one rule range and end in another.
     * @param positions The positions to filter
     * @param len The length of the pattern
     * @return All positions in the given array, that start and end in the same rule range
     *
     * @see #crossesBoundary(int, int)
     */
    private int[] inBoundary(int[] positions, int len) {
        IN_BOUNDARY_LIST.clear();
        for (int position : positions) {
            if (!crossesBoundary(position, position + len - 1)) {
                IN_BOUNDARY_LIST.add(position);
            }
        }

        return IN_BOUNDARY_LIST.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Calculates the index at which the range of the deepest nested rule, occupying the given index, starts.
     * @param index The index to check
     * @return The first index of the rule range that index is part of
     *
     */
    public int ruleIntervalStartIndex(int index) {
        Benchmark.startTimer(ALGORITHM_NAME, "ruleIntervalStartIndex");
        int current = intervalIndex.intervalStartIndex(index);
        Benchmark.stopTimer(ALGORITHM_NAME, "ruleIntervalStartIndex");

        return current;
    }


    /**
     * Marks an area as being part of the rule with the given id
     *
     * @param id   The rule id
     * @param from The start index of the range (inclusive)
     * @param to   The end index of the range (inclusive)
     *
     */
    public void markRange(int id, int from, int to) {
        var starts = ruleRangeStarts.get(from);
        if (starts == null) {
            starts = new TreeList<>();
            ruleRangeStarts.put(from, starts);
        }
        starts.add(new RuleIntervalStarts(id, from, to));
        intervalIndex.mark(id, from, to);
    }

    /**
     * Checks, whether an interval starts in one rule range, and ends in another
     * @param from The lower bound of the range
     * @param to The upper bound of the range (inclusive)
     * @return true, if the interval starts in the same rule range as it started. false otherwise
     */
    public boolean crossesBoundary(int from, int to) {
        // TODO
        //  The test to determine whether a substitution is legal must be adapted
        //  The issue is listed in the note at the top of this class
        //  If "from" is the index of the start of the deepest interval, a replacement can still take place
        //  The same is true for when "to" is the index of the end of the deepest interval
        //  This corresponds to having a non-terminal as the start/end position of the substring to replace
        Benchmark.startTimer(ALGORITHM_NAME, "crossesBoundary");
        RuleInterval fromInterval = intervalIndex.deepestIntervalAt(from);

        if (from > fromInterval.start() && to > fromInterval.end()) {
            return true;
        } else if (from == fromInterval.start()){
            while (from == fromInterval.start() && to > fromInterval.end()){
                fromInterval = fromInterval.parent();
            }
        }

        RuleInterval toInterval = intervalIndex.deepestIntervalAt(to);

        boolean b = !fromInterval.equals(toInterval);
        Benchmark.stopTimer(ALGORITHM_NAME, "crossesBoundary");
        return b;
    }

    private int nextRuleID() {
        return numRules++;
    }


    @Override
    public UnifiedRuleset toUnified() {

        // The Stack which contains all nested rule intervals at the current index.
        // The most deeply nested interval is at the top of the stack. The second deepest interval is the second element etc.
        Deque<RuleIntervalStarts> nestingStack = new ArrayDeque<>();

        // The Stack which contains the tentative symbol list of the rule which corresponds to the interval at the same
        // position in nestingStack.
        Deque<List<UnifiedSymbol>> symbolStack = new ArrayDeque<>();
        UnifiedRuleset ruleset = new UnifiedRuleset();

        final var chars = underlying.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            // If the index moved past the current rule interval, remove all intervals which ended and add the resulting rules
            // to the ruleset
            while (!nestingStack.isEmpty() && i > nestingStack.peek().end){
                final int id = nestingStack.pop().id;
                final List<UnifiedSymbol> symbols = symbolStack.pop();
                ruleset.putRule(id, symbols);

                if(!symbolStack.isEmpty()) {
                    symbolStack.peek().add(new UnifiedNonTerminal(id));
                }
            }

            // If new rules are starting at this index, add them to the stack to designate them as more deeply nested rules
            if(ruleRangeStarts.containsKey(i)) {
                for (RuleIntervalStarts interval : ruleRangeStarts.get(i)) {
                    nestingStack.push(interval);
                    symbolStack.push(new ArrayList<>());
                }
            }

            // Add the current char to the most deeply nested rule's stack
            char c = chars[i];
            symbolStack.peek().add(new UnifiedTerminal(c));
        }

        while (!nestingStack.isEmpty()){
            final int id = nestingStack.pop().id;
            final List<UnifiedSymbol> symbols = symbolStack.pop();
            ruleset.putRule(id, symbols);

            if(!symbolStack.isEmpty()) {
                symbolStack.peek().add(new UnifiedNonTerminal(id));
            }
        }


        return ruleset;
    }
}
