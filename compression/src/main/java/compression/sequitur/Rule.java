package compression.sequitur;

import compression.unified.UnifiedNonTerminal;
import compression.unified.UnifiedRuleset;
import compression.unified.UnifiedTerminal;
import compression.unified.interfaces.ToUnifiedRuleset;
import compression.unified.interfaces.UnifiedSymbol;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class Rule implements ToUnifiedRuleset {

    private static int NUM_RULES;

    private Guard guard;

    /**
     * Number used for identification of non-terminals
     */
    private final int number;


    /**
     * The amount of times this rule has been used
     */
    private int count;

    public Rule() {
        this.number = NUM_RULES;
        NUM_RULES++;
        guard = new Guard(this);
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

    @Override
    public UnifiedRuleset toUnified() {

        UnifiedRuleset ruleset = new UnifiedRuleset();
        ruleset.setTopLevelRuleId(number);

        final Function<Symbol, UnifiedSymbol> unify = symbol -> symbol instanceof NonTerminal nonTerminal ?
                new UnifiedNonTerminal(nonTerminal.getRule().number) :
                new UnifiedTerminal((char) symbol.value);

        // Contains all the rules that have been added to the queue already
        Set<Integer> processed = new HashSet<>();
        Queue<Rule> ruleQueue = new LinkedList<>();

        ruleQueue.add(this);
        processed.add(number);

        final var symbols = new ArrayList<UnifiedSymbol>();
        while(!ruleQueue.isEmpty()) {
            var currentRule = ruleQueue.poll();
            var currentSymbol = currentRule.first();

            while (currentSymbol != currentRule.guard) {
                if (currentSymbol instanceof NonTerminal nonTerminal && !processed.contains(nonTerminal.getRule().getNumber())) {
                    ruleQueue.add(nonTerminal.getRule());
                    processed.add(nonTerminal.getRule().number);
                }

                symbols.add(unify.apply(currentSymbol));
                currentSymbol = currentSymbol.next;
            }

            ruleset.putRule(currentRule.number, symbols);
            symbols.clear();
        }


        return ruleset;
    }

    /*@Override
    public String buildString() {
        Symbol current = guard;

        final Map<Integer, String> cache = new HashMap<>();
        final Deque<Symbol> stack = new ArrayDeque<>();
        final StringBuilder sb = new StringBuilder();

        while (current.next != guard) {
            final var next = current.next;
            if(next instanceof NonTerminal nonTerminal) {
                final var rule = nonTerminal.getRule();
                if(!cache.containsKey(rule.getNumber())) {
                    stack.push();
                }
            } else if (next instanceof Terminal) {
                sb.append((char) current.value);
            }
            current = next;
        }


        return null;
    }*/
}
