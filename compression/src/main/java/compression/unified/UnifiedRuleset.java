package compression.unified;

import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedSymbol;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * A compression.unified representation of a grammar
 */
public class UnifiedRuleset implements ToUnifiedRuleset {

    /**
     * The id of the start rule of this grammar
     */
    private int topLevelRuleId;

    /**
     * A map which take a rule ID and return the rule's right side as a list of symbols.
     */
    private final Int2ObjectMap<List<UnifiedSymbol>> rules;

    /**
     * Creates a new empty ruleset
     */
    public UnifiedRuleset() {
        rules = new Int2ObjectOpenHashMap<>();
    }

    public int getTopLevelRuleId() {
        return topLevelRuleId;
    }

    public void setTopLevelRuleId(int topLevelRuleId) {
        this.topLevelRuleId = topLevelRuleId;
    }

    public Int2ObjectMap<List<UnifiedSymbol>> rules() {
        return rules;
    }

    /**
     * Adds a rule to the ruleset
     * @param id The id of the rule
     * @param symbols The symbols of the rule's right side. They must be in the correct order
     */
    public void putRule(int id, Iterable<UnifiedSymbol> symbols) {
        rules.put(id, StreamSupport.stream(symbols.spliterator(), false).collect(Collectors.toList()));
    }

    /**
     * Gets the entire ruleset as a nicely formatted String
     * @return The Ruleset as a string
     */
    public String getAsString() {

        Map<Integer, String> map = new TreeMap<>();

        Set<Integer> processed = new HashSet<>();
        Map<Integer, Integer> usageCount = new HashMap<>();

        // Top level rule is possibly unused. So we prevent getting a null value
        usageCount.put(topLevelRuleId, 0);

        rules.int2ObjectEntrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).forEach(entry -> {
            int id = entry.getIntKey();
            var rule = new StringJoiner(" ", "R" + id + " -> ", "\n");

            for (UnifiedSymbol symbol : entry.getValue()) {
                if(symbol instanceof UnifiedNonTerminal nonTerminal) {
                    usageCount.merge(nonTerminal.id(), 1, Integer::sum);
                }
                rule.add(symbol.toString()
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace(' ', '_')
                );
            }

            if(!processed.contains(id)) {
                map.put(id, rule.toString());
                processed.add(id);
            }
        });

        var sb = new StringBuilder("Top level rule id: ").append(topLevelRuleId).append('\n');
        map.forEach((id, string) -> sb.append(" ").append(String.format("%-4d", usageCount.get(id))).append(string));
        return sb.toString();
    }

    /**
     * Gets the size of the grammar. This is calculated as the number of symbols of all rules' right side
     * @return The size of the grammar
     */
    public int rulesetSize() {
        return rules.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Gets the amount of rules
     * @return The amount of rules
     */
    public int ruleCount() {
        return rules.size();
    }

    /**
     * The average length of a rule, excluding the top level rule
     * @return The average length
     */
    public double averageRuleLength() {
        return rules.entrySet().stream().filter(entry -> entry.getKey() != topLevelRuleId)
                .map(Map.Entry::getValue)
                .mapToInt(List::size).average().orElse(0);
    }


    public int rulesetDepth() {
        var topLevel = rules.get(topLevelRuleId);
        int depth = 0;
        IntSet rule = topLevel.stream().filter(symbol -> symbol instanceof UnifiedNonTerminal)
                .mapToInt(symbol -> ((UnifiedNonTerminal) symbol).id())
                .collect(IntOpenHashSet::new, IntOpenHashSet::add, IntOpenHashSet::addAll);

        while (!rule.isEmpty()) {
            rule = rule.stream()// Get the nonterminals ids
                    .map(rules::get) // Get the rules according to the ids
                    .flatMap(List::stream) // Flat map them so that we get a single stream of symbols
                    .filter(symbol -> symbol instanceof UnifiedNonTerminal) // Retain only NonTerminals
                    .mapToInt(symbol -> ((UnifiedNonTerminal) symbol).id()) // Cast them to NonTerminals
                    .collect(IntOpenHashSet::new, IntOpenHashSet::add, IntOpenHashSet::addAll); // Collect them to a set so we only have distinct symbols left
            depth++;
        }
        return depth;
    }

    @Override
    public UnifiedRuleset toUnified() {
        return this;
    }
}
