package unified;

import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedSymbol;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * A unified representation of a grammar
 */
public class UnifiedRuleset implements ToUnifiedRuleset {

    /**
     * The id of the start rule of this grammar
     */
    private int topLevelRuleId;

    /**
     * A map which take a rule ID and return the rule's right side as a list of symbols.
     */
    private final Map<Integer, List<UnifiedSymbol>> rules;

    /**
     * Creates a new empty ruleset
     */
    public UnifiedRuleset() {
        rules = new TreeMap<>();
    }

    public int getTopLevelRuleId() {
        return topLevelRuleId;
    }

    public void setTopLevelRuleId(int topLevelRuleId) {
        this.topLevelRuleId = topLevelRuleId;
    }

    public Map<Integer, List<UnifiedSymbol>> rules() {
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

        rules.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).forEach(entry -> {
            Integer id = entry.getKey();
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
     * Check, whether the string represented by the ruleset matches the given string.
     * This is used to verify, that the grammar can be used to build the original string.
     * @param reference The reference string to compare the result against.
     * @return
     */
    public boolean verify(String reference) {
        if (reference == null) throw new IllegalArgumentException("String to compare to is null");
        return buildString().equals(reference);
    }

    /**
     * Builds the string represented by the grammar
     * @return The string represented by the grammar
     */
    public String buildString() {
        List<UnifiedSymbol> symbols = new ArrayList<>(rulesetSize());
        symbols.addAll(rules.get(topLevelRuleId));
        int i = 0;
        do {
            final var symbol = symbols.get(i);
            if(symbol instanceof UnifiedNonTerminal nonTerminal) {
                symbols.addAll(i + 1, rules.get(nonTerminal.id()));
                symbols.remove(i);
            } else {
                i++;
            }
        } while (i < symbols.size());

        return symbols.stream()
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    @Override
    public UnifiedRuleset toUnified() {
        return this;
    }
}
