package areacomp.v1;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class Rule implements CharSequence, Iterable<Symbol>{


    /**
     * This rule's Guard symbol
     */
    private final Guard guard;

    /**
     * This rule's id, used for identification of non-terminals representing this rule and this rule
     */
    private final int id;

    /**
     * The amount of times this rule is being used
     */
    private int useCount;

    /**
     * The length of the rule if fully expanded
     */
    private int expandedLength;

    /**
     * The amount of Symbols in this rule's right side
     */
    private int length;

    /**
     * The ruleset which this rule belongs to
     */
    private final Ruleset ruleset;

    /**
     * Creates a new Top Level rule for a string and inserts it into the ruleset's map
     * @param s The string for which the rule should be created
     * @param r The ruleset which this Rule is a part of
     */
    public Rule(String s, Ruleset r) {
        this(r);
        for (char c : s.toCharArray()) {
            last().insertAfter(new Terminal(c));
        }
        ruleset.getRuleMap().put(id, this);
        expandedLength = s.length();
        length = s.length();
    }

    /**
     * Create a new empty rule. This is used internally to create new rules which replace subsequences
     * @param r The ruleset which this rule belongs to
     */
    private Rule(Ruleset r) {
        this.ruleset = r;
        this.id = ruleset.nextRuleID();
        guard = new Guard(this);
        this.useCount = 0;
    }

    /**
     * Factorizes a repeated sequence. All occurence of the pattern in the grammar will be replaced with a new rule
     * This method makes the following assumptions
     * All occurences are non overlapping.
     * The positions are sorted in ascending order
     * @param len The length of the subsequence to replace
     * @param positions The start position of the occurences
     */
    public void factorize(int len, int... positions) {
        // If the occurence is less than one
        if (len <= 1 || positions.length < 2) return;
        var current = first();
        // Go to the first occurence
        current = current.step(positions[0]);
        // We step 1 before the occurrence, so we can substitute it, without ruining our pointer into the sequence
        current = current.prev;

        // The factor is substituted len times and replaced by 1 non-terminal
        // That means this rule's right side gets shorter for each replacement.
        this.length -= (len - 1) * positions.length;

        // Create a new rule
        var rule = new Rule(ruleset);
        rule.length = len;

        // Substitute the sequence with the new rule. The removed subsequence is returned
        var interval = current.next.substitute(rule, len);

        var first = interval.start();

        // Calculate the length of the new Rule, if it were to be completely expanded
        // The expanded lengths of all the symbols making the rule up, are added
        var expandedLength = 0;

        // As the interval was cut from the parent rule, first will be null at the end of the subsequence
        while (first != null) {
            expandedLength += first.expandedLength();

            if (first.isNonTerminal()) {
                ((NonTerminal) first).getRule().useCount++;
            }

            // Insert the Symbols into the new rule
            var temp = first.detachNext();
            rule.last().insertAfter(first);
            first = temp;
        }

        rule.expandedLength = expandedLength;

        // Iterate through the positions and substitute each occurrence.
        for (int i = 1; i < positions.length; i++) {

            // The amount of steps forward is the distance between two succeeding positions with the length of the removed
            // subsequence subtracted
            final int stepDistance = positions[i] - positions[i - 1] - len + 1;
            var next = current.step(stepDistance);
            next.next.substitute(rule, len);
            current = next;
        }

        // Scan the entire grammar to find instances of this rule's right side and replace them with this rule
        ruleset.substituteAllOccurences(rule);

        // Put this rule into the map
        ruleset.getRuleMap().put(rule.id, rule);
    }

    /**
     * Returns A list of symbols at which non-overlapping occurrences of the other rule's right side start in this rule
     * @param other A rule to be searched for
     * @return A list of start symbols at which the right side of the rule can be found
     */
    public List<Symbol> getRuleIndices(Rule other) {
        var list = new ArrayList<Symbol>();
        int n = other.length;
        int currentMatch = 0;
        int i = 0;

        // The start symbol of the next potential match
        Symbol matchStart = null;

        Symbol current = other.first();
        for(var symbol : this) {
            if (symbol.value == current.value) {
                if(currentMatch == 0) matchStart = symbol;
                currentMatch++;
                current = current.next;
            } else {
                currentMatch = 0;
                current = other.first();
            }
            if(currentMatch == n) {
                list.add(matchStart);
                currentMatch = 0;
                current = other.first();
            }
            i++;
        }

        return list;
    }


    public Symbol first() {
        return guard.next;
    }

    public Symbol last() {
        return guard.prev;
    }

    public void incrementUseCount() {
        useCount++;
    }

    public void decrementUseCount() {
        useCount--;
    }

    public int getId() {
        return id;
    }

    public void decreaseLength(int amount) {
        length -= amount;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return (char) first().step(index).value;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        var current = first().step(start);
        var sb = new StringBuilder();
        for (int i = start; i < end - 1; i++) {
            sb.append((char) current.value);
            current = current.next;
            if(current == guard) throw new IndexOutOfBoundsException(i);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        for(var sym : this) {
            if (sym.isTerminal()) {
                sb.append((char) sym.value).append(" ");
            } else if (sym.isNonTerminal()) {
                sb.append("R").append(sym.value - NonTerminal.NUM_TERMINALS).append(" ");
            }
        }

        return String.format(" %d R%-3s -> %s", useCount, id, sb.toString().replace("\n", "\\n"));
    }

    public String getAllRules() {
        var ruleMap = new HashMap<Integer, String>();
        var queue = new ArrayDeque<Rule>();
        queue.add(this);

        while(!queue.isEmpty()) {
            var currentRule = queue.poll();
            var current = currentRule.first();
            var sb = new StringBuilder();

            // The rule has already been handled
            if (ruleMap.containsKey(currentRule.id)) continue;

            while (current != currentRule.guard) {
                if (current.isTerminal()) {
                    if ((char) current.value == ' ') {
                        sb.append('_');
                    } else {
                        sb.append((char) current.value);
                    }

                    sb.append(' ');
                } else if (current instanceof NonTerminal nonTerminal) {
                    sb.append("R").append(nonTerminal.getRule().id).append(" ");
                    queue.add(nonTerminal.getRule());
                }
                current = current.next;
            }
            ruleMap.put(currentRule.id, String.format("  %-2d  R%-3s -> %s", currentRule.useCount, currentRule.id, sb.toString().replace("\n", "\\n")));
            sb.append("\n");
        }

        var list = ruleMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        list.add(0, "Usage         Rule");
        list.add("Rule set size: " + ruleset.ruleSetSize());

        return String.join("\n", list);
    }


    public int expandedLength() {
        return expandedLength;
    }

    @Override
    public Iterator<Symbol> iterator() {
        return new RuleIterator();
    }

    public Stream<Symbol> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    private class RuleIterator implements Iterator<Symbol> {

        private Symbol current;

        public RuleIterator() {
            this.current = first();
        }

        @Override
        public boolean hasNext() {
            return current != guard;
        }

        @Override
        public Symbol next() {
            var temp = current;
            current = current.next;
            return temp;
        }
    }
}
