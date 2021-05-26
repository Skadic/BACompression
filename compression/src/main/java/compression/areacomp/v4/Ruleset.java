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
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.*;

@SuppressWarnings("Duplicates")
class Ruleset implements ToUnifiedRuleset {

    public static final String ALGORITHM_NAME = AreaCompV4.class.getSimpleName();

    /**
     * The string for which this ruleset is built
     */
    private final String underlying;

    private final RuleIntervalIndex intervalIndex;

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
            int[] positions = Arrays.stream(augS.getSuffixArray(), areaData.low - 1, areaData.high + 1)
                    .toArray();

            // Get the length of the longest common prefix in this range of the lcp array
            // This will be the length of the pattern that is to be replaced.
            int len = areaData.len;

            // This means there is no repeated subsequence of 2 or more characters. In this case, abort
            if (len <= 1) {
                break;
            }

            Arrays.sort(positions);

            Benchmark.startTimer(ALGORITHM_NAME, "in boundary");
            int positionCount = cleanPositions(positions, len);
            if (positionCount <= 1) {
                Benchmark.stopTimer(ALGORITHM_NAME, "positions");
                Benchmark.stopTimer(ALGORITHM_NAME, "in boundary");
                continue;
            }

            Benchmark.startTimer(ALGORITHM_NAME, "multiple occurrences");
            var multipleOccurrences = differingOccurences(positions);
            Benchmark.stopTimer(ALGORITHM_NAME, "multiple occurrences");

            Benchmark.stopTimer(ALGORITHM_NAME, "positions");

            if(!multipleOccurrences) {
                continue;
            }
            Benchmark.startTimer(ALGORITHM_NAME, "factorize");
            substitute(len, positions);
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
    public void substitute(int len, int... positions) {
        // If the occurence is less than one or
        if (len <= 1 || positions.length < 2) return;

        final int nextId = nextRuleID();

        // Iterate through the positions and substitute each occurrence.
        for (int position : positions) {
            if(position == -1) continue;

            Benchmark.startTimer(ALGORITHM_NAME, "markRange");
            intervalIndex.mark(nextId, position, position + len - 1);
            Benchmark.stopTimer(ALGORITHM_NAME, "markRange");
        }
    }

    /**
     * Filters out the occurrences of the pattern with length len and at the given positions, which start in one rule range and end in another.
     * Also filters out overlapping occurrences.
     * @param positions The positions to filter
     * @param len The length of the pattern
     * @return The amount of valid positions
     *
     * @see #substitutionAllowed(int, int)
     */
    private int cleanPositions(int[] positions, int len) {
        int count = 0;
        int previous = Short.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            if (previous + len <= pos && substitutionAllowed(pos, pos + len - 1)) {
                previous = pos;
                count++;
            } else {
                positions[i] = -1;
            }
        }

        return count;
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
        var set = new IntOpenHashSet();

        int firstRuleId = -1;


        // Count how often this pattern appears at specific indices inside the right side of a rule
        // E.g. if the String is "abacdabac" and only the Rule "R1 -> abac" exists so far (which implies the structure "R1 d R1")
        // then the pattern "c" appears twice in the string in rule 1 at index 3
        // The corresponding map entry would then be (1, (3, 2))
        for (var pos : positions) {
            if(pos == -1) continue;

            final var ruleInterval = intervalIndex.intervalContaining(pos);
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

    /**
     * Checks whether the given Interval can be substituted without violating other previous substitutions in this area.
     * In essence, this checks whether there is an already substituted interval in which a start symbol and and end symbol can be found,
     * which correspond to the indices from and to in the input String.
     *
     * For example. Take the following grammar for the string "abacaba":
     *
     * S -> AcA
     * A -> aba
     *
     * This method returns true for the input [0, 3]
     * This is because the 0 index here corresponds to either the first "A" in Rule S or the first "a" in Rule A and the 3 index corresponds to the "c", both in Rule S.
     * Here, a start- and end symbol can be found that are in the same rule interval and correspond to the indices. These being the "A" and "c" in Rule S respectively.
     *
     * This method returns false for the input [1, 3], since the only symbol that corresponds to the index 1 is the "b" in rule A, and for index 3 it is only the "c".
     * So there is no way to find a start- and end symbol which both lie in the same substituted interval.
     *
     * This method also returns false for the input [0, 5]. Index 0 corresponds to the first "A" in rule S and the first "a" in rule A, while index 5 corresponds only to the "b" in rule A.
     * While start- and end symbols can be found, which are of the same rule ("a" and "b" of rule A), they are *not* of the same substituted interval.
     * While the "a" is of the first substituted instance of rule A, the "b" is of the second substituted instance. As a result this substitution would also be invalid.
     *
     * @param from The lower bound of the range (inclusive)
     * @param to The upper bound of the range (inclusive)
     * @return true, if the interval starts in the same rule range as it started. false otherwise
     */
    public boolean substitutionAllowed(int from, int to) {
        Benchmark.startTimer(ALGORITHM_NAME, "crossesBoundary");
        RuleInterval fromInterval = intervalIndex.intervalContaining(from);

        // If the start index is the start of this interval, it might imply that this is a non-terminal in a less-deeply nested rule.
        // In that case, we iterate upwards by using the parent pointers in order to find the first interval, which contains the end index also.
        // Mind, that only intervals that start at the same index, and the parent of the last such interval can be considered here.
        // This is because while the start index is equal to the current interval's start position, that means that the start index
        // describes the position of a non-terminal in its parent interval.
        while (from == fromInterval.start() && to > fromInterval.end() && fromInterval.hasParent()){
            fromInterval = fromInterval.parent();
        }


        // If there is no such interval, that would also contain "to", the substitution can't be allowed
        if(to > fromInterval.end()) {
            return false;
        }

        RuleInterval toInterval = intervalIndex.intervalContaining(to);

        while (!(toInterval.contains(fromInterval)) && to == toInterval.end() && toInterval.hasParent()) {
            toInterval = toInterval.parent();
        }


        boolean b = fromInterval.equals(toInterval);
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
        Deque<RuleInterval> nestingStack = new ArrayDeque<>();

        // The Stack which contains the tentative symbol list of the rule which corresponds to the interval at the same
        // position in nestingStack.
        Deque<List<UnifiedSymbol>> symbolStack = new ArrayDeque<>();
        UnifiedRuleset ruleset = new UnifiedRuleset();

        // Cache maps for Symbols, so they can be reused if they have already been created for a symbol
        Int2ObjectMap<UnifiedNonTerminal> nonTerminals = new Int2ObjectOpenHashMap<>();
        Char2ObjectMap<UnifiedTerminal> terminals = new Char2ObjectOpenHashMap<>();


        for (int i = 0; i < underlying.length(); i++) {
            // If the index moved past the current rule interval, remove all intervals which ended and add the resulting rules
            // to the ruleset
            while (!nestingStack.isEmpty() && i > nestingStack.peek().end()){
                final int id = nestingStack.pop().ruleId();
                final List<UnifiedSymbol> symbols = symbolStack.pop();
                ruleset.putRule(id, symbols);

                if(!symbolStack.isEmpty()) {
                    symbolStack.peek().add(nonTerminals.computeIfAbsent(id, UnifiedNonTerminal::new));
                }
            }

            // If new rules are starting at this index, add them to the stack to designate them as more deeply nested rule
            RuleInterval intervalAt = intervalIndex.intervalAtStartIndex(i);

            if(intervalAt != null) {
                intervalAt = intervalAt.firstAtStartIndex();
                for (Iterator<RuleInterval> it = intervalAt.deeperIterator(); it.hasNext(); ) {
                    RuleInterval interval = it.next();
                    nestingStack.push(interval);
                    symbolStack.push(new ArrayList<>());
                }
            }


            // Add the current char to the most deeply nested rule's stack
            char c = underlying.charAt(i);
            symbolStack.peek().add(terminals.computeIfAbsent(c, _c -> new UnifiedTerminal(c)));
        }

        while (!nestingStack.isEmpty()){
            final int id = nestingStack.pop().ruleId();
            final List<UnifiedSymbol> symbols = symbolStack.pop();
            ruleset.putRule(id, symbols);

            if(!symbolStack.isEmpty()) {
                symbolStack.peek().add(new UnifiedNonTerminal(id));
            }
        }


        return ruleset;
    }
}
