package compression.areacomp.v3;

import compression.areacomp.AreaFunction;
import compression.unified.UnifiedNonTerminal;
import compression.unified.UnifiedRuleset;
import compression.unified.UnifiedTerminal;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedSymbol;
import compression.utils.AugmentedString;
import compression.utils.Benchmark;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class Ruleset implements ToUnifiedRuleset {

    public static final String ALGORITHM_NAME = AreaCompV3.class.getSimpleName();

    /**
     * The string for which this ruleset is built
     */
    private final String underlying;

    /**
     * This rule set's start rule
     */
    private final Rule topLevelRule;

    private final RuleIntervalIndex intervalIndex;

    /**
     * A map that contains all rules. It maps from a rule ID to its corresponding rule
     */
    private final Map<Integer, Rule> ruleMap = new HashMap<>();

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

        this.topLevelRule = new Rule(s, this);
        intervalIndex = new RuleIntervalIndex(topLevelRule.getId(), s.length());
        underlying = s;
        Benchmark.stopTimer(ALGORITHM_NAME, "construction");
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
            if(data.area > 0) queue.add(data);
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
            final var multipleOccurrences = multipleDifferingOccurences(positions);
            Benchmark.stopTimer(ALGORITHM_NAME, "multiple occurrences");

            Benchmark.stopTimer(ALGORITHM_NAME, "positions");

            if(positions.length <= 1 || !multipleOccurrences) {
                continue;
            }

            Benchmark.startTimer(ALGORITHM_NAME, "factorize");
            topLevelRule.factorize(len, positions);
            Benchmark.stopTimer(ALGORITHM_NAME, "factorize");
        }
        Benchmark.stopTimer(ALGORITHM_NAME, "total time");
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
    private boolean multipleDifferingOccurences(int[] positions) {
        if(positions.length == 0) return false;
        // Map from rule id -> index in the right side of the rule -> amount of occurrences
        var map = new HashMap<Integer, HashMap<Integer, Integer>>();

        // Map from rule area start -> rule id -> occurrence count
        var newMap = new HashMap<Integer, Integer>();

        // Count how often this pattern appears at specific indices inside the right side of a rule
        // E.g. if the String is "abacdabac" and only the Rule "R1 -> abac" exists so far (which implies the structure "R1 d R1")
        // then the pattern "c" appears twice in the string in rule 1 at index 3
        // The corresponding map entry would then be (1, (3, 2))
        for (var pos : positions) {
            final var id = getDeepestRuleIdAt(pos);
            final var indexInRule = pos - ruleRangeStartIndex(pos);
            final var ruleArea = ruleRangeStartIndex(pos);

            if(!map.containsKey(id)) map.put(id, new HashMap<>());
            map.get(id).merge(indexInRule, 1, Integer::sum);

            newMap.merge(ruleArea, 1, Integer::sum);
        }


        return newMap.values().stream().reduce(Integer.MIN_VALUE, Integer::max) > 1;
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
            if (!crossesBoundary(position, position + len)) {
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
    public int ruleRangeStartIndex(int index) {
        long now = System.nanoTime();
        int current = intervalIndex.startInterval(index).totalStart();
        Benchmark.updateTime(ALGORITHM_NAME, "ruleRangeStartIndex", System.nanoTime() - now);

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
        intervalIndex.mark(id, from, to);
    }

    /**
     * Checks, whether an interval starts in one rule range, and ends in another
     * @param from The lower bound of the range
     * @param to The upper bound of the range (inclusive)
     * @return true, if the interval starts in the same rule range as it started. false otherwise
     */
    public boolean crossesBoundary(int from, int to) {
        Benchmark.startTimer(ALGORITHM_NAME, "crossesBoundary");

        var fromInterval = intervalIndex.intervalAt(from);
        var toInterval = intervalIndex.intervalAt(to - 1);

        boolean b = fromInterval.ruleId() != toInterval.ruleId() || fromInterval.totalStart() != toInterval.totalStart();
        Benchmark.stopTimer(ALGORITHM_NAME, "crossesBoundary");
        return b;
    }

    /**
     * Gets the id of the most deeply nested rule which occupies this index
     *
     * @param index The given index
     * @return The index of the rule
     */
    public int getDeepestRuleIdAt(int index) {
        return intervalIndex.deepestRuleAt(index);
    }

    /**
     * Gets the most deeply nested rule which occupies this index
     *
     * @param index The given index
     * @return The rule
     *
     * @see #getDeepestRuleAt(int)
     */
    public Rule getDeepestRuleAt(int index) {
        return ruleMap.get(getDeepestRuleIdAt(index));
    }

    /**
     * Gets a collection of all rules in this set
     *
     * @return All rules in this set
     */
    public Collection<Rule> rules() {
        return ruleMap.values();
    }

    /**
     * Get a map that maps Rule IDs to their corresponding rule
     *
     * @return The map of IDs to rule
     */
    public Map<Integer, Rule> getRuleMap() {
        return ruleMap;
    }

    /**
     * Gets the next number to be used for a created rule id
     *
     * @return A new unused rule id
     */
    public int nextRuleID() {
        return numRules++;
    }

    /**
     * Gets the rule in the set which represents the start rule
     *
     * @return The rule set's start rule
     */
    public Rule getTopLevelRule() {
        return topLevelRule;
    }

    /**
     * Gets the size of this rule set.
     * The size is calculated as the sum of characters in all rule's right sides.
     *
     * @return The rule set's size
     */
    public long ruleSetSize() {
        return rules().stream().flatMap(Rule::stream).count();
    }

    @Override
    public UnifiedRuleset toUnified() {

        Function<Symbol, UnifiedSymbol> unify = symbol -> {
            if(symbol instanceof NonTerminal nonTerminal) {
                return new UnifiedNonTerminal(nonTerminal.getRule().getId());
            } else {
                return new UnifiedTerminal((char) symbol.value());
            }
        };

        UnifiedRuleset ruleset = new UnifiedRuleset();

        ruleset.setTopLevelRuleId(topLevelRule.getId());

        for(var rule : rules()) {
            ruleset.putRule(rule.getId(), rule.stream().map(unify).collect(Collectors.toList()));
        }

        return ruleset;
    }
}
