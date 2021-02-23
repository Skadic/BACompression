package areacomp.v2;

import areacomp.AreaFunction;
import unified.UnifiedNonTerminal;
import unified.UnifiedRuleset;
import unified.UnifiedTerminal;
import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedSymbol;
import utils.AugmentedString;
import utils.Benchmark;
import utils.Interval;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Ruleset implements ToUnifiedRuleset {

    public static final String ALGORITHM_NAME = AreaCompV2.class.getSimpleName();

    /**
     * The string for which this ruleset is built
     */
    private final String underlying;

    /**
     * This rule set's start rule
     */
    private final Rule topLevelRule;

    /**
     * Index structure for deciding which rule a certain character in the original string is part of
     * The invariant for this list is, that for each internal list, the list contains the ids of rules
     * in the order of how deeply they are nested.
     * For example, take the grammar:
     * R0 -> R2 c R2
     * R1 -> aa
     * R2 -> R1 b R1
     * Then the full String is aabaacaabaa. The ruleRanges list for index 1 (the second 'a') would be:
     * [0, 2, 1], as from the root (R0) one would need to travel through an R2 and an R1, to arrive at this character
     */
    private final List<List<Integer>> ruleRanges;

    /**
     * Records at which points rules start
     */
    private final List<HashSet<Integer>> ruleRangeStarts;

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
        var now = System.nanoTime();

        this.topLevelRule = new Rule(s, this);
        ruleRanges = new ArrayList<>(s.length());
        ruleRangeStarts = new ArrayList<>(s.length());
        // Populate the data structures
        for (int i = 0; i < s.length(); i++) {
            // At the start, each position is occupied only by rule 0 (the top level rule)
            var list = new ArrayList<Integer>();
            list.add(0);
            ruleRanges.add(list);
            // There is no index, at which a rule starts yet, except...
            ruleRangeStarts.add(new HashSet<>());
        }
        // ... for index 0, at which rule 0 starts
        ruleRangeStarts.get(0).add(0);
        underlying = s;
        Benchmark.updateTime(ALGORITHM_NAME, "construction", System.nanoTime() - now);
    }

    /**
     * Compresses the ruleset using an area function.
     * The area function determines how the intervals in the priority queue are prioritised.
     *
     * @param fun The area function used to prioritise intervals in the lcp array
     */
    public void compress(AreaFunction fun) {
        var nowTotal = System.nanoTime();
        var now = nowTotal;
        final var augS = new AugmentedString(underlying);
        Benchmark.updateTime(ALGORITHM_NAME, "suffix array", System.nanoTime() - now);

        now = System.nanoTime();
        // Create a Priority Queue which holds possible intervals in the LCP array
        // The priority value is calculated through the given area function.
        // This function should return a high value for promising intervals in the LCP array.
        var queue = new PriorityQueue<Interval>(
                Comparator.comparingInt(
                        interval -> -fun.area(augS.getSuffixArray(), augS.getInverseSuffixArray(), augS.getLcp(), interval.start(), interval.end()).area
                )
        );

        // Add all possible intervals
        for (int i = 1; i <= augS.length(); i++) {
            for (int j = i + 1; j <= augS.length(); j++) {
                queue.add(new Interval(i, j));
            }
        }
        Benchmark.updateTime(ALGORITHM_NAME, "queue", System.nanoTime() - now);

        while (true) {

            now = System.nanoTime();
            // Poll the best interval
            var interval = queue.poll();

            // The positions at which the pattern can be found
            int[] positions = IntStream.range(interval.start() - 1, interval.end())
                    .map(augS::suffixIndex)
                    .toArray();

            // Get the length of the longest common prefix in this range of the lcp array
            // This will be the length of the pattern that is to be replaced.
            int len = IntStream.range(interval.start(), interval.end())
                    .map(augS::lcp)
                    .min()
                    .orElse(0);

            // This means there is no repeated subsequence of 2 or more characters. In this case, abort
            if (len <= 1) {
                break;
            }

            Arrays.sort(positions);

            positions = nonOverlapping(positions, len);
            positions = inBoundary(positions, len);

            final var multipleOccurrences = multipleDifferingOccurences(positions);

            Benchmark.updateTime(ALGORITHM_NAME, "positions", System.nanoTime() - now);

            if(positions.length <= 1 || !multipleOccurrences) {
                continue;
            }


            now = System.nanoTime();
            topLevelRule.factorize(len, positions);
            Benchmark.updateTime(ALGORITHM_NAME, "factorize", System.nanoTime() - now);
        }
        Benchmark.updateTime(ALGORITHM_NAME, "total time", System.nanoTime() - nowTotal);
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

    /**
     * Filters out the occurrences of the pattern with length len and at the given positions, which start in one rule range and end in another.
     * @param positions The positions to filter
     * @param len The length of the pattern
     * @return All positions in the given array, that start and end in the same rule range
     *
     * @see #crossesBoundary(int, int)
     */
    private int[] inBoundary(int[] positions, int len) {
        List<Integer> list = new ArrayList<>();
        for (int position : positions) {
            if (!crossesBoundary(position, position + len)) {
                list.add(position);
            }
        }

        return list.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Calculates the index at which the range of the deepest nested rule, occupying the given index, starts.
     * @param index The index to check
     * @return The first index of the rule range that index is part of
     *
     * @see #ruleRanges
     * @see #ruleRangeStarts
     */
    public int ruleRangeStartIndex(int index) {
        int current = index;
        int maxId = getDeepestRuleIdAt(index);
        while (!ruleRangeStarts.get(current).contains(maxId)) {
            current--;
            if (current == -1) return 0;
        }

        return current;
    }

    /**
     * Marks an area as being part of the rule with the given id
     *
     * @param id   The rule id
     * @param from The start index of the range (inclusive)
     * @param to   The end index of the range (exclusive)
     *
     * @see #ruleRanges
     * @see #ruleRangeStarts
     */
    public void markRange(int id, int from, int to) {
        // Get the id of the rule that occupied this area before
        final int originalId = getDeepestRuleIdAt(from);
        // Add the start of the new rule's area to the list
        ruleRangeStarts.get(from).add(id);

        // Add the id of the new rule into the range list after the id of the original rule
        // This serves to always have the deepest nested Rule at a certain index to be the last index of the list in ruleRanges
        for (int i = from; i < to; i++) {
            var index = ruleRanges.get(i).indexOf(originalId);
            ruleRanges.get(i).add(index + 1, id);
        }
    }

    /**
     * Checks, whether an interval starts in one rule range, and ends in another
     * @param from The lower bound of the range
     * @param to The upper bound of the range (inclusive)
     * @return true, if the interval starts in the same rule range as it started. false otherwise
     */
    public boolean crossesBoundary(int from, int to) {
        return getDeepestRuleIdAt(from) != getDeepestRuleIdAt(to - 1) || ruleRangeStartIndex(from) != ruleRangeStartIndex(to - 1);
    }

    /**
     * Removes all overlapping occurences of a pattern from an array of positions
     *
     * @param positions     All occurences of the pattern
     * @param patternLength The length of the pattern
     * @return The occurences of the pattern with overlapping occurences removed
     */
    private static int[] nonOverlapping(int[] positions, int patternLength) {
        final List<Integer> list = new ArrayList<>();
        int last = Short.MIN_VALUE;
        for (Integer i : positions) {
            if (i - last >= patternLength) {
                list.add(i);
                last = i;
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Gets the id of the most deeply nested rule which occupies this index
     *
     * @param index The given index
     * @return The index of the rule
     *
     * @see #ruleRanges
     */
    public int getDeepestRuleIdAt(int index) {
        List<Integer> ruleIdList = ruleRanges.get(index);
        return ruleIdList.get(ruleIdList.size() - 1);
    }

    /**
     * Gets the most deeply nested rule which occupies this index
     *
     * @param index The given index
     * @return The rule
     *
     * @see #ruleRanges
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
