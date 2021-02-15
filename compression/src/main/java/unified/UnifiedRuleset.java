package unified;

import unified.interfaces.ToUnifiedRuleset;
import unified.interfaces.UnifiedSymbol;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class UnifiedRuleset implements ToUnifiedRuleset {

    private int topLevelRuleId;
    private final Map<Integer, List<UnifiedSymbol>> rules;

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

    public void putRule(int id, Iterable<UnifiedSymbol> symbols) {
        rules.put(id, StreamSupport.stream(symbols.spliterator(), false).collect(Collectors.toList()));
    }

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
                    usageCount.merge(nonTerminal.getId(), 1, Integer::sum);
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

    public long rulesetSize() {
        return rules.values().stream().mapToLong(List::size).sum();
    }

    public boolean verify(String reference) {
        if (reference == null) throw new IllegalArgumentException("String to compare to is null");

        List<UnifiedSymbol> symbols = new ArrayList<>(rules.get(topLevelRuleId));
        List<UnifiedSymbol> buffer = new ArrayList<>(symbols.size());

        boolean existNonTerminal = true;
        while (existNonTerminal) {
            existNonTerminal = false;
            for (UnifiedSymbol symbol : symbols) {
                if (symbol instanceof UnifiedNonTerminal nonTerminal) {
                    buffer.addAll(rules.get(nonTerminal.getId()));
                    existNonTerminal = true;
                } else {
                    buffer.add(symbol);
                }
            }
            symbols.clear();
            symbols.addAll(buffer);
            buffer.clear();
        }

        String result = symbols.stream()
                .map(Object::toString)
                .collect(Collectors.joining());


        System.out.println("Result length: " + result.length());
        System.out.println("Reference length: " + reference.length());
        System.out.println();
        System.out.println("Differing indices: ");

        System.out.println(IntStream.range(0, Math.min(result.length(), reference.length()))
                .filter(i -> result.charAt(i) != reference.charAt(i)).boxed()
                .collect(Collectors.toList()));
        System.out.println();

        if(!reference.equals(result)) {
            System.out.println("Reference:");
            System.out.println(reference);
            System.out.println();
            System.out.println("Result:");
            System.out.println(result);
        }

        System.out.println();
        return result.equals(reference);
    }

    @Override
    public UnifiedRuleset toUnified() {
        return this;
    }
}
