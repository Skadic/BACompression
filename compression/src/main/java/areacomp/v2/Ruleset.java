package areacomp.v2;

import areacomp.AreaFunction;
import org.javatuples.Pair;
import unified.UnifiedNonTerminal;
import unified.UnifiedRuleset;
import unified.UnifiedTerminal;
import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedSymbol;
import utils.AugmentedString;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Ruleset implements ToUnifiedRuleset {

    /**
     * The string for which this ruleset is built
     */
    private String underlying;

    /**
     * This rule set's start rule
     */
    private final Rule topLevelRule;

    /**
     * Index structure for deciding which rule a certain character in the original string is part of
     */
    private final List<List<Integer>> ruleRanges;

    /**
     * Records at which points rules start
     */
    private final List<HashSet<Integer>> ruleAreaStarts;

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
        this.topLevelRule = new Rule(s, this);
        ruleRanges = new ArrayList<>(s.length());
        ruleAreaStarts = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            var list = new ArrayList<Integer>();
            list.add(0);
            ruleRanges.add(list);
            ruleAreaStarts.add(new HashSet<>());
        }
        ruleAreaStarts.get(0).add(0);
        underlying = s;
    }

    /**
     * Compresses the ruleset using an area function.
     * The area function determines how the intervals in the priority queue are prioritised.
     *
     * @param fun The area function used to prioritise intervals in the lcp array
     */
    public void compress(AreaFunction fun) {
        final var augS = new AugmentedString(underlying);

        // Create a Priority Queue which holds possible intervals in the LCP array
        // The priority value is calculated through the given area function.
        // This function should return a high value for promising intervals in the LCP array.
        var queue = new PriorityQueue<Pair<Integer, Integer>>(
                Comparator.comparingInt(
                        r -> -fun.area(augS.getSuffixArray(), augS.getInverseSuffixArray(), augS.getLcp(), r.getValue0(), r.getValue1())
                )
        );

        // Add all possible intervals
        for (int i = 1; i <= augS.length(); i++) {
            for (int j = i + 1; j <= augS.length(); j++) {
                queue.add(new Pair<>(i, j));
            }
        }

        while (true) {

            // Poll the best interval
            var interval = queue.poll();

            // The positions at which the pattern can be found
            int[] positions = IntStream.range(interval.getValue0() - 1, interval.getValue1())
                    .map(augS::suffixIndex)
                    .toArray();

            // Get the length of the longest common prefix in this range of the lcp array
            // This will be the length of the pattern that is to be replaced.
            int len = IntStream.range(interval.getValue0(), interval.getValue1()).map(augS::lcp).min().orElse(0);

            // This means there is no repeated subsequence of 2 or more characters. In this case, abort
            if (len <= 1) {
                break;
            }

            Arrays.sort(positions);

            positions = nonOverlapping(positions, len);
            positions = inBoundary(positions, len);

            final var differingOccurences = countDifferingOccurences(positions);

            if(positions.length <= 1 || differingOccurences <= 1) {
                continue;
            }

            topLevelRule.factorize(len, positions);

            /*System.out.println(topLevelRule.getCumulativeLength());
            System.out.println(ruleIndex);
            print();
            System.out.println();*/
        }
    }

    private int countDifferingOccurences(int[] positions) {
        var set = Arrays.stream(positions)
                .mapToObj(pos -> {
                    final var id = getMaxRuleIdAt(pos);
                    final var indexInRule = pos - ruleAreaStartIndex(pos);
                    return new RuleIndex(id, indexInRule);
                })
                .collect(Collectors.toSet());
        return set.size();
    }

    private int[] inBoundary(int[] positions, int len) {
        List<Integer> list = new ArrayList<>();
        for (int position : positions) {
            if (!crossesBoundary(position, position + len)) {
                list.add(position);
            }
        }

        return list.stream().mapToInt(i -> i).toArray();
    }

    public int ruleAreaStartIndex(int index) {
        int current = index;
        int maxId = getMaxRuleIdAt(index);
        while (!ruleAreaStarts.get(current).contains(maxId)) {
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
     */
    public void markRange(int id, int from, int to) {
        ruleAreaStarts.get(from).add(id);
        for (int i = from; i < to; i++) {
            ruleRanges.get(i).add(id);
        }
    }

    public boolean crossesBoundary(int from, int to) {
        int temp = getMaxRuleIdAt(from);
        for (int i = from + 1; i < to; i++) {
            if (getMaxRuleIdAt(i) != temp) return true;
        }
        return false;
    }

    /**
     * Removes all overlapping occurences of a pattern from an array of positions
     *
     * @param positions     All occurences of the pattern
     * @param patternLength The length of the pattern
     * @return The occurences of the pattern with overlapping occurences removed
     */
    private static int[] nonOverlapping(int[] positions, int patternLength) {
        List<Integer> list = new ArrayList<>();
        int last = Short.MIN_VALUE;
        for (Integer i : positions) {
            if (i - last >= patternLength) {
                list.add(i);
                last = i;
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    public boolean hasRule(int index, int id) {
        return ruleRanges.get(index).contains(id);
    }

    public int getMaxRuleIdAt(int index) {
        List<Integer> ruleIdList = ruleRanges.get(index);
        return ruleIdList.get(ruleIdList.size() - 1);
    }

    public Rule getRuleAt(int index) {
        return ruleMap.get(getMaxRuleIdAt(index));
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

    /**
     * Builds the original String from the ruleset
     *
     * @return The original String, which this ruleset produces
     */
    public String reconstruct() {
        boolean done = false;
        while (!done) {
            done = true;

            // Repeatedly iterate through the top level rule
            // Every Non-terminal that is found is expanded until no more non-terminals are found
            for (Symbol sym : topLevelRule.stream().collect(Collectors.toList())) {
                if (sym.isNonTerminal()) {
                    ((NonTerminal) sym).expand();
                    done = false;
                }
            }
        }

        // Reset this ruleset to its original state
        ruleMap.clear();
        ruleMap.put(0, topLevelRule);
        numRules = 1;

        return topLevelRule.subSequence(0, topLevelRule.length());
    }

    /**
     * Nicely prints this ruleset
     */
    public void print() {
        System.out.println(topLevelRule.getAllRules());
    }

    public List<List<Integer>> getRuleRanges() {
        return ruleRanges;
    }

    @Override
    public UnifiedRuleset toUnified() {

        Function<Symbol, UnifiedSymbol> unify = symbol -> {
            if(symbol instanceof NonTerminal nonTerminal) {
                return new UnifiedNonTerminal(nonTerminal.getRule().getId());
            } else {
                return new UnifiedTerminal((char) symbol.value);
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
