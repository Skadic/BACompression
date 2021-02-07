package areacomp.v2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class Rule implements CharSequence, Iterable<Symbol> {

    /**
     * The list of symbols that make up the right side of this rule
     */
    private final List<Symbol> symbols;

    /**
     * A prefix sum of the length of the expanded rule up to this index in its right side. For example:
     *
     * Let A -> BBde, B -> abc. And if the former was this rule. Then the list is:
     * [3, 6, 7, 8]
     */
    private final List<Integer> cumulativeLength;

    /**
     * This rule's id, used for identification of non-terminals representing this rule and this rule
     */
    private final int id;

    /**
     * The amount of times this rule is being used
     */
    private int useCount;

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
        // Add all chars in the string to the Symbols list
        this(s.chars()
                .mapToObj(Terminal::new)
                .collect(Collectors.toList()),
                r
        );

        int stringLength = s.length();

        // The cumulative sum array in the beginning is just the list of ascending natural numbers up until the length of the string
        // because every symbol has length 1
        IntStream.range(1, stringLength + 1)
                .boxed()
                .forEach(cumulativeLength::add);

        ruleset.getRuleMap().put(id, this);
    }

    /**
     * Create a new rule consisting of the given symbols. This is used internally to create new rules which replace subsequences
     * Note that this constructor does *not* construct the {@link #cumulativeLength} list.
     * @param symbols The symbols this rule makes up
     * @param r The ruleset which this rule belongs to
     */
    private Rule(List<Symbol> symbols, Ruleset r) {
        this.ruleset = r;
        this.id = ruleset.nextRuleID();
        this.symbols = new ArrayList<>(symbols);
        this.cumulativeLength = new ArrayList<>(symbols.size());
        this.useCount = 0;
    }

    private Rule(Ruleset r) {
        this(new ArrayList<>(), r);
    }

    private static record SubstitutionData(List<Symbol> symbolList, List<Integer> cumulativeLengthList ) {}

    public SubstitutionData substitute(Rule rule, int start, int end) {
        var symbolList = new ArrayList<Symbol>(end - start);
        var cumulative = new ArrayList<Integer>(end - start);
        var baseCumulativeLength = start > 0 ? cumulativeLength.get(start - 1) : 0;

        for (int i = start; i < end; i++) {
            symbolList.add(symbols.get(i));
            cumulative.add(cumulativeLength.get(i) - baseCumulativeLength);
        }

        symbols.subList(start + 1, end).clear();
        symbols.set(start, new NonTerminal(rule));
        cumulativeLength.subList(start, end - 1).clear();

        return new SubstitutionData(symbolList, cumulative);
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

        var processed = new HashSet<RuleIndex>();

        // Create a new rule
        final var rule = new Rule(ruleset);

        final var firstRelevantRuleIndex = ruleset.ruleAreaStartIndex(positions[0]);
        final var firstRelevantRule = ruleset.getRuleAt(positions[0]);
        final var firstPosition = firstRelevantRule.searchTerminalIndex(positions[0] - firstRelevantRuleIndex);
        final var substitutionData = firstRelevantRule.substitute(rule, firstPosition, firstPosition + len);
        processed.add(new RuleIndex(firstRelevantRule.id, positions[0] - firstRelevantRuleIndex));

        ruleset.markRange(rule.id, positions[0], positions[0] + len);
        rule.symbols.addAll(substitutionData.symbolList());
        rule.cumulativeLength.addAll(substitutionData.cumulativeLengthList());

        // As the interval was cut from the parent rule, first will be null at the end of the subsequence
        for (Symbol current : rule) {
            if (current.isNonTerminal()) {
                ((NonTerminal) current).getRule().useCount++;
            }
        }

        // Iterate through the positions and substitute each occurrence.
        for (int i = 1; i < positions.length; i++) {
            final var relevantRule = ruleset.getRuleAt(positions[i]);
            final var relevantRuleIndex = ruleset.ruleAreaStartIndex(positions[i]);
            RuleIndex ruleIndex = new RuleIndex(relevantRule.id, positions[i] - relevantRuleIndex);

            if(!processed.contains(ruleIndex)) {
                final var position = relevantRule.searchTerminalIndex(positions[i] - relevantRuleIndex);
                relevantRule.substitute(rule, position, position + len);
                processed.add(ruleIndex);
            }
            ruleset.markRange(rule.id, positions[i], positions[i] + len);
        }

        // Put this rule into the map
        ruleset.getRuleMap().put(rule.id, rule);
    }

    public List<Integer> getCumulativeLength() {
        return Collections.unmodifiableList(cumulativeLength);
    }

    /**
     * Returns A list of symbols at which non-overlapping occurrences of the other rule's right side start in this rule
     * @param other A rule to be searched for
     * @return A list of start symbols at which the right side of the rule can be found
     */
    public List<Symbol> getRuleIndices(Rule other) {
        var list = new ArrayList<Symbol>();
        int n = other.ruleLength();
        int currentMatch = 0;
        int i = 0;

        // The start symbol of the next potential match
        Symbol matchStart = null;

        for(var symbol : this) {
            if (symbol.value == other.symbols.get(i).value) {
                if(currentMatch == 0) matchStart = symbol;
                currentMatch++;
                i++;
            } else {
                currentMatch = 0;
                i = 0;
            }
            if(currentMatch == n) {
                list.add(matchStart);
                currentMatch = 0;
                i = 0;
            }
        }

        return list;
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

    public int ruleLength() {
        return symbols.size();
    }

    @Override
    public int length() {
        return cumulativeLength.get(ruleLength() - 1);
    }

    @Override
    public char charAt(int index) {
        final var localIndex  = searchTerminalIndex(index);
        if (symbols.get(localIndex) instanceof NonTerminal nonTerminal) {
            final var cumulativeLengthBefore = localIndex > 0 ? cumulativeLength.get(localIndex - 1) : 0;
            return nonTerminal.getRule().charAt(index - cumulativeLengthBefore);
        } else {
            return (char) symbols.get(index).value;
        }
    }

    @Override
    public String subSequence(int start, int end) {

        // Let A -> BBBde, B -> abc
        // [3, 6, 9, 10, 11]
        // Want 4, 8
        // startSymbolIndex = 1;
        // endSymbolIndex =  2;
        final int startSymbolIndex = searchTerminalIndex(start);
        final int endSymbolIndex = searchTerminalIndex(end - 1);


        // The first and last symbol need to be handled separately,
        // since they can start in the middle of a non-terminal's rule

        final String firstSegment;
        // If it is a terminal then there is no further processing needed
        if (symbols.get(startSymbolIndex) instanceof Terminal terminal) {
            firstSegment = String.valueOf((char) terminal.value);
        } else {
            // If it is a non-terminal the index in the Non-Terminal's rule at which the pattern starts
            // needs to be determined
            final var nonTerminal = (NonTerminal) symbols.get(startSymbolIndex);
            final int lengthBeforeStartSymbol = startSymbolIndex > 0 ? cumulativeLength.get(startSymbolIndex - 1) : 0;
            final int startIndexInStartSymbol = start - lengthBeforeStartSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            firstSegment = nonTerminalRule.subSequence(startIndexInStartSymbol, nonTerminalRule.length());
        }

        final String lastSegment;
        if (symbols.get(endSymbolIndex) instanceof Terminal terminal) {
            lastSegment = String.valueOf((char) terminal.value);
        } else {
            final var nonTerminal = (NonTerminal) symbols.get(endSymbolIndex);
            final int lengthBeforeEndSymbol = endSymbolIndex > 0 ? cumulativeLength.get(endSymbolIndex - 1) : 0;
            final int endIndexInEndSymbol = end - lengthBeforeEndSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            lastSegment = nonTerminalRule.subSequence(endIndexInEndSymbol, nonTerminalRule.length());
        }

        final var sb = new StringBuilder(firstSegment);

        IntStream.range(startSymbolIndex + 1, endSymbolIndex)
                .mapToObj(symbols::get)
                .map(symbol -> {
                    if(symbol.isTerminal()) {
                        return symbol.toString();
                    } else {
                        var sym = (NonTerminal) symbol;
                        Rule symbolRule = sym.getRule();
                        return symbolRule.subSequence(0, symbolRule.length());
                    }
                })
                .forEach(sb::append);

        sb.append(lastSegment);

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
            var sb = new StringBuilder();

            // The rule has already been handled
            if (ruleMap.containsKey(currentRule.id)) continue;

            for (var current : currentRule) {
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

    @Override
    public Iterator<Symbol> iterator() {
        return symbols.iterator();
    }

    public Stream<Symbol> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * A binary search for the symbol in {@link #symbols} that contains the terminal at the given index
     * in the fully expanded form of this rule's right side. For example:
     * Given the grammar A -> aBef, B -> bcd and if this {@link Rule} was the former rule.
     * The result of the search for index 3, would return 'd', since the fully expanded form of this rule's right side would be:
     * 'abcdef'.
     * @param index The index to search for
     * @return The index of the symbol in {@link #symbols} that contains the terminal
     */
    public int searchTerminalIndex(int index) {
        if(index < 0 || index >= length()) return -1;

        // The borders of the search space
        int lowBorder = 0;
        int highBorder = ruleLength() - 1;
        while (true) {
            int middle = (highBorder + lowBorder) / 2;
            int low = middle - 1 >= 0 ? cumulativeLength.get(middle - 1) : 0;
            int high = cumulativeLength.get(middle);

            if (low <= index && index < high) {
                return middle;
            } else if (index < low) {
                highBorder = middle - 1;
            } else {
                lowBorder = middle + 1;
            }
        }
    }

    public Iterator<Symbol> iteratorBetween(int from, int to) {
        return new SymbolIntervalIterator(from ,to);
    }

    public Stream<Symbol> streamBetween(int from, int to) {
        return StreamSupport.stream(((Iterable<Symbol>) () -> iteratorBetween(from, to)).spliterator(), false);
    }

    private class SymbolIntervalIterator implements Iterator<Symbol> {

        private int current;
        private final int end;

        public SymbolIntervalIterator(int from, int to) {
            current = from;
            end = to;
        }

        @Override
        public boolean hasNext() {
            return current < end;
        }

        @Override
        public Symbol next() {
            return symbols.get(current++);
        }
    }
}
