package areacomp.v3;

import org.jetbrains.annotations.NotNull;
import utils.Benchmark;
import utils.RuleLocalIndex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("Duplicates")
public class Rule implements CharSequence, Iterable<Symbol> {

    private final NavigableMap<Integer, Symbol> symbolMap;

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
     * @param s The char sequence for which the rule should be created
     * @param r The ruleset which this Rule is a part of
     */
    public Rule(CharSequence s, Ruleset r) {
        // Add all chars in the string to the Symbols list
        this(IntStream.range(0, s.length())
                        .mapToObj(s::charAt)
                        .map(Terminal::new)
                        .collect(Collectors.toList()),
                        r
        );
    }

    /**
     * Create a new rule consisting of the given symbols. This is used internally to create new rules which replace subsequences
     * @param symbolSource The symbols this rule makes up
     * @param r The ruleset which this rule belongs to
     */
    private Rule(List<Symbol> symbolSource, Ruleset r) {
        this.ruleset = r;
        this.id = ruleset.nextRuleID();
        this.symbolMap = new TreeMap<>();
        this.useCount = 0;
        ruleset.getRuleMap().put(id, this);

        int len = 0;
        for (Symbol symbol : symbolSource) {
            symbolMap.put(len, symbol);
            len += symbol.terminalLength();
        }
    }

    private Rule(Ruleset r) {
        this(new ArrayList<>(), r);
    }

    /**
     * A class meant to hold the data that comes as a result of substitution
     */
    private static record SubstitutionData(List<Symbol> symbolList, List<Integer> cumulativeLengthList ) {}

    /**
     * Substitutes the symbols in the given interval with the given rule and return.
     * Note that the length are in reference to the pattern's fully expanded length, and *not* just the amount of symbols
     * @param rule The rule to replace the sequence with
     * @param start The start index of the rule
     * @param length The length (in terminal symbols) of the pattern to replace
     * @return The list of replaced symbols and the prefix sum over the lengths of each symbol in said list
     */
    public List<Symbol> substitute(Rule rule, int start, int length) {
        var now = System.nanoTime();
        var symbolList = new ArrayList<>(symbolMap.subMap(start, start + length).values());
        Benchmark.updateTime("Rule", "substitute", System.nanoTime() - now);

        replace(rule, start, length);

        return symbolList;
    }

    public void replace(Rule rule, int start, int length) {
        var now = System.nanoTime();
        symbolMap.subMap(start, start + length).clear();
        symbolMap.put(start, new NonTerminal(rule));
        Benchmark.updateTime("Rule", "sublist clear", System.nanoTime() - now);
    }

    /**
     * Factorizes a repeated sequence. All occurence of the pattern in the grammar will be replaced with a new rule
     * This method makes the following assumptions
     * All occurences are non overlapping.
     * The positions are sorted in ascending order.
     * This method may only be called on the top level rule of the grammar.
     * @param len The length of the subsequence to replace in terminal characters
     * @param positions The start positions of the occurences in the original string
     */
    public void factorize(int len, int... positions) {
        // If the occurence is less than one or
        if (len <= 1 || positions.length < 2) return;

        var processed = new HashSet<RuleLocalIndex>();
        // Create a new rule
        final var rule = new Rule(ruleset);

        Benchmark.startTimer("Rule", "firstrule");
        // Replace the first occurrence of the rule
        final var firstRelevantRuleIndex = ruleset.ruleRangeStartIndex(positions[0]);
        final var firstRelevantRule = ruleset.getDeepestRuleAt(positions[0]);
        final var symbolList = firstRelevantRule.substitute(rule, positions[0] - firstRelevantRuleIndex, len);
        processed.add(new RuleLocalIndex(firstRelevantRule.id, positions[0] - firstRelevantRuleIndex));

        // Mark the area now occupied by this rule with the rule's id
        ruleset.markRange(rule.id, positions[0], positions[0] + len - 1);

        // Populate the rule with data
        rule.addSymbols(symbolList);
        Benchmark.stopTimer("Rule", "firstrule");

        Benchmark.startTimer("Rule", "adjust usecount");
        // Since all the occurrences of this rule's right side will be replaced in the string
        // We increase the use count of each non terminal by one for each time it appears in the right side of the new rule
        // That way all the non-terminal will have a proper usage count
        for (Symbol current : rule) {
            if (current instanceof NonTerminal) {
                ((NonTerminal) current).getRule().useCount++;
            }
        }
        Benchmark.stopTimer("Rule", "adjust usecount");

        // Iterate through the positions and substitute each occurrence.
        for (int i = 1; i < positions.length; i++) {
            final Rule deepestRule = ruleset.getDeepestRuleAt(positions[i]);
            final int deepestRuleRangeIndex = ruleset.ruleRangeStartIndex(positions[i]);
            RuleLocalIndex ruleLocalIndex = new RuleLocalIndex(deepestRule.id, positions[i] - deepestRuleRangeIndex);
            // Each specific occurrence of a rule in a certain context (represented by ruleLocalIndex) should only be replaced once
            // If this pattern appears in a rule that has been factorized already, then it might still appear multiple times in the original string
            // but only once in the grammar.
            if(!processed.contains(ruleLocalIndex)) {
                deepestRule.replace(rule, ruleLocalIndex.index(), len);
                processed.add(ruleLocalIndex);
            }
            Benchmark.startTimer("Rule", "markRange");
            ruleset.markRange(rule.id, positions[i], positions[i] + len - 1);
            Benchmark.stopTimer("Rule", "markRange");
        }

        // Put this rule into the map
        //ruleset.getRuleMap().put(rule.id, rule);
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

    /**
     * The amount of symbols in this rule's right side
     * @return The amount of symbols
     */
    public int symbolCount() {
        return symbolMap.size();
    }

    @Override
    public int length() {
        return lengthAt(symbolCount() - 1);
    }

    @Override
    public char charAt(int index) {
        final var localIndex  = searchTerminalIndex(index);
        if (symbolAtLocalIndex(localIndex) instanceof NonTerminal nonTerminal) {
            final var cumulativeLengthBefore = lengthUpTo(localIndex);
            return nonTerminal.getRule().charAt(index - cumulativeLengthBefore);
        } else {
            return (char) symbolAtLocalIndex(localIndex).value();
        }
    }

    @Override
    public String subSequence(int start, int end) {

        if(end <= start) {
            return "";
        } else if (start + 1 == end) {
            return String.valueOf(charAt(start));
        }

        final int startSymbolIndex = searchTerminalIndex(start);
        final int endSymbolIndex = searchTerminalIndex(end - 1);
        final int length = end - start;


        // The first and last symbol need to be handled separately,
        // since they can start in the middle of a non-terminal's rule

        final String firstSegment;
        // If it is a terminal then there is no further processing needed
        if (symbolAtLocalIndex(startSymbolIndex) instanceof Terminal terminal) {
            firstSegment = String.valueOf((char) terminal.value());
        } else {
            // If it is a non-terminal the index in the Non-Terminal's rule at which the pattern starts
            // needs to be determined
            final var nonTerminal = (NonTerminal) symbolAtLocalIndex(startSymbolIndex);
            final int lengthBeforeStartSymbol = lengthUpTo(startSymbolIndex);
            final int startIndexInStartSymbol = start - lengthBeforeStartSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            firstSegment = nonTerminalRule.subSequence(startIndexInStartSymbol, Math.min(nonTerminalRule.length(), startIndexInStartSymbol + length));
        }

        if (firstSegment.length() == length) return firstSegment;

        final String lastSegment;
        if (symbolAtLocalIndex(endSymbolIndex) instanceof Terminal terminal) {
            lastSegment = String.valueOf((char) terminal.value());
        } else {
            final var nonTerminal = (NonTerminal) symbolAtLocalIndex(endSymbolIndex);
            final int lengthBeforeEndSymbol = lengthUpTo(endSymbolIndex);
            final int endIndexInEndSymbol = end - lengthBeforeEndSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            lastSegment = nonTerminalRule.subSequence(endIndexInEndSymbol, Math.min(nonTerminalRule.length(), endIndexInEndSymbol + length));
        }

        if(firstSegment.length() + lastSegment.length() == length) return  firstSegment + lastSegment;

        final var sb = new StringBuilder(firstSegment);

        //IntStream.range(startSymbolIndex + 1, endSymbolIndex)
                //.mapToObj(symbols::get)
        symbolMap.subMap(startSymbolIndex + 1, endSymbolIndex).values().stream()
                .map(symbol -> {
                    if(symbol instanceof Terminal) {
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

    @NotNull
    @Override
    public String toString() {
        var sb = new StringBuilder();

        for(var sym : this) {
            if (sym instanceof Terminal) {
                final String s = switch (sym.value()) {
                    case ' ' -> "_";
                    case '\n' -> "\\n";
                    default -> String.valueOf((char) sym.value());
                };
                sb.append(s).append(" ");
            } else if (sym instanceof NonTerminal) {
                sb.append("R").append(sym.value()).append(" ");
            }
        }

        return String.format(" %d R%-3s -> %s", useCount, id, sb.toString().replace("\n", "\\n"));
    }

    @NotNull
    @Override
    public Iterator<Symbol> iterator() {
        return symbolMap.values().iterator();
    }

    public Stream<Symbol> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * A search for the symbol in {@link #symbolMap} that contains the terminal at the given index
     * in the fully expanded form of this rule's right side. For example:
     * Given the grammar A -> aBef, B -> bcd and if this {@link Rule} was the former rule.
     * The result of the search for index 3, would return 'd', since the fully expanded form of this rule's right side would be:
     * 'abcdef'.
     * @param index The index to search for
     * @return The index of the symbol in {@link #symbolMap} that contains the terminal
     */
    public int searchTerminalIndex(int index) {
        return symbolMap.floorKey(index);
    }

    /**
     * Calculates the length of the rule in terminals up to that index (exclusively)
     * @param index The index to check
     * @return The length
     */
    private int lengthUpTo(int index) {
        if(index == symbolMap.size()) {
            final var last = symbolMap.lastEntry();
            return last.getKey() + last.getValue().terminalLength();
        }
        // FIXME Linear time is not cool
        for (Integer len : symbolMap.keySet()) {
            if(index-- == 0) return len;
        }
        return -1;
    }


    /**
     * Calculates the length of the rule in terminals at that index (inclusively)
     * @param index The index to check
     * @return The length
     */
    private int lengthAt(int index) {
        if(index == 0) return symbolMap.get(0).terminalLength();
        // FIXME Linear time is not cool
        for (var entry : symbolMap.entrySet()) {
            if(index-- == 0) return entry.getKey() + entry.getValue().terminalLength();
        }
        return -1;
    }

    private Symbol symbolAtLocalIndex(int index) {
        return symbolMap.get(index);
    }

    private Symbol symbolContainingTerminalAt(int index) {
        return symbolMap.floorEntry(index).getValue();
    }

    private void addSymbols(Collection<Symbol> newSymbols) {
        final var lastEntry = symbolMap.lastEntry();
        var len = lastEntry == null ? 0 : lastEntry.getKey() + lastEntry.getValue().terminalLength();

        for (Symbol symbol : newSymbols) {
            symbolMap.put(len, symbol);
            len += symbol.terminalLength();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule symbols = (Rule) o;
        return id == symbols.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
