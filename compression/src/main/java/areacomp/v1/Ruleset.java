package areacomp.v1;

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

    public static final String ALGORITHM_NAME = AreaCompV1.class.getSimpleName();

    /**
     * This rule set's start rule
     */
    private final Rule topLevelRule;

    /**
     * A map that contains all rules. It maps from a rule ID to its corresponding rule
     */
    private final Map<Integer, Rule> ruleMap = new HashMap<>();

    /**
     * Amount of rules in the set. This is used for getting new rule IDs in {@link #nextRuleID()}
     */
    private int numRules;

    /**
     * Creates a new ruleset from a String. A rule that produces this string directly is created.
     * @param s The string for which to create the Ruleset
     */
    public Ruleset(String s) {
        this.topLevelRule = new Rule(s, this);
    }

    /**
     * Compresses the ruleset using an area function.
     * The area function determines how the intervals in the
     * @param fun The Area function that determines which intervals in the LCP array are prioritized
     */
    public void compress(AreaFunction fun) {
        final long totalNow = System.nanoTime();
        boolean unchanged = false;
        while (!unchanged) {
            unchanged = true;
            var ruleList = new ArrayList<>(rules());
            for (var rule : ruleList) {
                if(rule.length() <= 3) continue;

                var now = System.nanoTime();
                final var augS = new AugmentedString(rule);
                Benchmark.updateTime(ALGORITHM_NAME, "suffix array", System.nanoTime() - now);


                now = System.nanoTime();
                PriorityQueue<Interval> intervals = new PriorityQueue<>(
                        Comparator.comparingInt(
                                interval -> -fun.area(augS, interval.start(), interval.end()).area
                        )
                );

                // Get the maximum valued interval
                for (int i = 1; i <= augS.length(); i++) {
                    for (int j = i + 1; j <= augS.length(); j++) {
                        intervals.add(new Interval(i, j));
                    }
                }
                Benchmark.updateTime(ALGORITHM_NAME, "queue", System.nanoTime() - now);

                now = System.nanoTime();


                Interval interval;
                int[] positions;
                int len;

                // Poll intervals from the queue, until one with more than 1 occurrence and pattern length > 1 is found
                do {
                    interval = intervals.poll();

                    // The positions at which the pattern can be found
                    positions = IntStream.range(interval.start() - 1, interval.end())
                            .map(augS::suffixIndex)
                            .toArray();

                    // Get the length of the longest common prefix in this range of the lcp array
                    // This will be the length of the pattern that is to be replaced.
                    len = IntStream.range(interval.start(), interval.end()).map(augS::lcp).min().orElse(0);

                    // This means there is no repeated subsequence of 2 or more characters. In this case, abort
                    if(len > 1) {
                        unchanged = false;
                    } else {
                        continue;
                    }

                    Arrays.sort(positions);
                    positions = nonOverlapping(positions, len);
                    Benchmark.updateTime(ALGORITHM_NAME, "positions", System.nanoTime() - now);

                    if (positions.length > 1) {
                        break;
                    }
                } while(!intervals.isEmpty());

                    if (positions.length <= 1) {
                    break;
                }

                now = System.nanoTime();
                rule.factorize(len, positions);
                Benchmark.updateTime(ALGORITHM_NAME, "factorize", System.nanoTime() - now);
            }
        }
        Benchmark.updateTime(ALGORITHM_NAME, "total time", System.nanoTime() - totalNow);
    }

    /**
     * Removes all overlapping occurences of a pattern from an array of positions
     * @param positions All occurences of the pattern
     * @param patternLength The length of the pattern
     * @return The occurences of the pattern with overlapping occurences removed
     */
    private static int[] nonOverlapping(int[] positions, int patternLength) {
        List<Integer> list = new ArrayList<>();
        int last = Short.MIN_VALUE;
        for (Integer i : positions) {
            if(i - last >= patternLength) {
                list.add(i);
                last = i;
            }
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Searches for all occurences of this rule's right side and replaces it with the rule's corresponding non-terminal
     * @param other The rule whose right side to search for
     */
    public void substituteAllOccurences(Rule other) {
        int n = other.length();

        for (var rule : ruleMap.values()) {
            // Skip this rule, if it's the same as the other rule
            // Otherwise we would basically delete the right side of the original rule
            if(rule == other) continue;

            var occurences = rule.getRuleIndices(other);

            for (Symbol occurence : occurences) {
                occurence.substitute(other, n);
                rule.decreaseLength(n - 1);
            }
        }
    }

    /**
     * Gets a collection of all rules in this set
     * @return All rules in this set
     */
    public Collection<Rule> rules() {
        return ruleMap.values();
    }

    /**
     * Get a map that maps Rule IDs to their corresponding rule
     * @return The map of IDs to rule
     */
    public Map<Integer, Rule> getRuleMap() {
        return ruleMap;
    }

    /**
     * Gets the next number to be used for a created rule id
     * @return A new unused rule id
     */
    public int nextRuleID() {
        return numRules++;
    }

    /**
     * Gets the rule in the set which represents the start rule
     * @return The rule set's start rule
     */
    public Rule getTopLevelRule() {
        return topLevelRule;
    }

    /**
     * Gets the size of this rule set.
     * The size is calculated as the sum of characters in all rule's right sides.
     * @return The rule set's size
     */
    public long ruleSetSize() {
        return rules().stream().flatMap(Rule::stream).count();
    }

    /**
     * Builds the original String from the ruleset
     * @return The original String, which this ruleset produces
     */
    public String reconstruct() {
        boolean done = false;
        while(!done) {
            done = true;

            // Repeatedly iterate through the top level rule
            // Every Non-terminal that is found is expanded until no more non-terminals are found
            for(Symbol sym : topLevelRule.stream().collect(Collectors.toList())) {
                if (sym.isNonTerminal()) {
                    ((NonTerminal) sym).expand();
                    done = false;
                }
            }
        }

        // Reset this ruleset to its original state
        topLevelRule.setLength(topLevelRule.expandedLength());
        ruleMap.clear();
        ruleMap.put(0, topLevelRule);
        numRules = 1;

        return topLevelRule.subSequence(0, topLevelRule.expandedLength()).toString();
    }

    @Override
    public UnifiedRuleset toUnified() {
        UnifiedRuleset ruleset = new UnifiedRuleset();
        ruleset.setTopLevelRuleId(0);

        Queue<Rule> ruleQueue = new ArrayDeque<>();
        HashSet<Integer> processed = new HashSet<>();
        processed.add(0);

        Function<Symbol, UnifiedSymbol> unify = symbol -> {
            if(symbol instanceof NonTerminal nonTerminal) {
                return new UnifiedNonTerminal(nonTerminal.getRule().getId());
            } else {
                return new UnifiedTerminal((char) symbol.value);
            }
        };

        ruleset.putRule(0, topLevelRule.stream()
                .map(unify)
                .collect(Collectors.toList())
        );

        ruleQueue.add(topLevelRule);

        while(!ruleQueue.isEmpty()) {
            var currentRule = ruleQueue.poll();

            ruleset.putRule(currentRule.getId(), currentRule.stream()
                    .peek(symbol -> {
                        if (symbol instanceof NonTerminal nonTerminal && !processed.contains(nonTerminal.getRule().getId())) {
                            ruleQueue.add(nonTerminal.getRule());
                            processed.add(nonTerminal.getRule().getId());
                        }
                    })
                    .map(unify)
                    .collect(Collectors.toList())
            );
        }

        return ruleset;
    }
}
