package sequitur;

import java.util.*;
import java.util.stream.Collectors;

public class Rule {

    private static int NUM_RULES;

    private Guard guard;

    /**
     * Number used for identification of non-terminals
     */
    private final int number;

    /**
     * Used for printing
     */
    private int index;

    /**
     * The amount of times this rule has been used
     */
    private int count;

    public Rule() {
        this.number = NUM_RULES;
        NUM_RULES++;
        guard = new Guard(this);
        this.index = 0;
        this.count = 0;
    }

    public Symbol first() {
        return guard.next;
    }

    public Symbol last() {
        return guard.prev;
    }

    public void incrementCount() {
        count++;
    }

    public void decrementCount() {
        count--;
    }

    public int getNumber() {
        return number;
    }

    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    public Guard getGuard() {
        return guard;
    }

    public static void resetNumRules() {
        NUM_RULES = 0;
    }

    public int count() {
        return count;
    }

    public String getRulez() {
        var rules = new ArrayList<Rule>(NUM_RULES);

        Rule currentRule;
        Rule referedTo;
        Symbol sym;

        int index;
        int processedRules = 0;
        var text = new StringBuilder();
        int charCounter = 0;

        text.append("Usage\tRule\n");
        rules.add(this);

        while (processedRules < rules.size()) {
            currentRule = rules.get(processedRules);
            text.append(" ");
            text.append(currentRule.count);
            text.append("\tR");
            text.append(processedRules);
            text.append(" -> ");

            for(sym = currentRule.first(); !sym.isGuard(); sym = sym.next) {
                if(sym.isNonTerminal()) {
                    referedTo = ((NonTerminal) sym).getRule();
                    if((rules.size() > referedTo.index) && (rules.get(referedTo.index) == referedTo)) {
                        index = referedTo.index;
                    } else {
                        index = rules.size();
                        referedTo.index = index;
                        rules.add(referedTo);
                    }
                    text.append('R');
                    text.append(index);
                } else {
                    if (sym.value == ' ') {
                        text.append('_');
                    } else {
                        if (sym.value == '\n') {
                            text.append("\\n");
                        } else {
                            text.append((char) sym.value);
                        }
                    }
                }
                text.append(' ');
            }
            text.append('\n');
            processedRules++;
        }
        text.append("Rule set size: ").append(ruleSetSize());
        return text.toString();
    }

    public String getRules() {
        var ruleMap = new HashMap<Integer, String>();
        var queue = new ArrayDeque<Rule>();
        queue.add(this);

        while (!queue.isEmpty()) {
            var currentRule = queue.poll();
            var current = currentRule.first();
            var sb = new StringBuilder();

            // The rule has already been handled
            if (ruleMap.containsKey(currentRule.number)) continue;

            while (current != currentRule.guard) {
                if (current.isTerminal()) {
                    if ((char) current.value == ' ') {
                        sb.append('_');
                    } else {
                        sb.append((char) current.value);
                    }
                    sb.append(" ");
                } else if (current instanceof NonTerminal nonTerminal) {
                    sb.append("R").append(nonTerminal.getRule().number).append(" ");
                    queue.add(nonTerminal.getRule());
                }
                current = current.next;
            }

            ruleMap.put(currentRule.number, String.format("  %-2d  R%-3s -> %s", currentRule.count, currentRule.number, sb.toString()
                    .replace("\n", "\\n")
            ));
            sb.append("\n");
        }

        var list = ruleMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        list.add(0, "Usage         Rule");
        list.add("Rule set size: " + ruleSetSize());
        return String.join("\n", list);
    }

    public long ruleSetSize() {
        var ruleSet = new HashSet<Rule>();
        var queue = new ArrayDeque<Rule>();
        ruleSet.add(this);
        queue.add(this);

        int count = 0;
        while (!queue.isEmpty()) {
            var currentRule = queue.poll();
            var currentSymbol = currentRule.first();
            while (currentSymbol != currentRule.guard) {
                count++;

                if (currentSymbol.isNonTerminal()) {
                    var nextRule = ((NonTerminal) currentSymbol).getRule();
                    if(!ruleSet.contains(nextRule)) {
                        ruleSet.add(nextRule);
                        queue.add(nextRule);
                    }
                }
                currentSymbol = currentSymbol.next;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        var sym = first();
        while (sym != guard) {
            if (sym.isTerminal()) {
                sb.append((char) sym.value).append(" ");
            } else if (sym instanceof NonTerminal nonTerminal) {
                sb.append("R").append(nonTerminal.getRule().number).append(" ");
            }
            sym = sym.next;
        }

        return String.format(" %d R%-3s -> %s", count, number, sb.toString().replace("\n", "\\n"));
    }
}
