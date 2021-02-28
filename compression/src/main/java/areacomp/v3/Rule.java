package areacomp.v3;

import utils.RuleLocalIndex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Rule implements CharSequence, Iterable<Symbol> {

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

        int stringLength = s.length();

        // The cumulative sum array in the beginning is just the list of ascending natural numbers up until the length of the string
        // because every symbol has length 1
        IntStream.range(1, stringLength + 1)
                .boxed()
                .forEach(cumulativeLength::add);
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
        ruleset.getRuleMap().put(id, this);
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
    public SubstitutionData substitute(Rule rule, int start, int length) {
        var symbolList = new ArrayList<Symbol>(length);
        var cumulative = new ArrayList<Integer>(length);
        // The "start index" of this
        var baseCumulativeLength = start > 0 ? cumulativeLength.get(start - 1) : 0;

        var remainingLength = length;
        for (int i = start; remainingLength > 0; i++) {
            symbolList.add(symbols.get(i));
            cumulative.add(cumulativeLength.get(i) - baseCumulativeLength);
            remainingLength -= cumulativeLength.get(i) - (i > 0 ? cumulativeLength.get(i - 1) : 0);
        }

        symbols.subList(start + 1, start + symbolList.size()).clear();
        symbols.set(start, new NonTerminal(rule));
        cumulativeLength.subList(start, start + symbolList.size() - 1).clear();

        return new SubstitutionData(symbolList, cumulative);
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
        // If the occurence is less than one
        if (len <= 1 || positions.length < 2) return;

        var processed = new HashSet<RuleLocalIndex>();

        // Create a new rule
        final var rule = new Rule(ruleset);

        // Replace the first occurrence of the rule
        final var firstRelevantRuleIndex = ruleset.ruleRangeStartIndex(positions[0]);
        final var firstRelevantRule = ruleset.getDeepestRuleAt(positions[0]);
        final var firstPosition = firstRelevantRule.searchTerminalIndex(positions[0] - firstRelevantRuleIndex);
        final var substitutionData = firstRelevantRule.substitute(rule, firstPosition, len);
        processed.add(new RuleLocalIndex(firstRelevantRule.id, positions[0] - firstRelevantRuleIndex));

        // Mark the area now occupied by this rule with the rule's id
        ruleset.markRange(rule.id, positions[0], positions[0] + len - 1);

        // Populate the rule with data
        rule.symbols.addAll(substitutionData.symbolList());
        rule.cumulativeLength.addAll(substitutionData.cumulativeLengthList());

        // Since all the occurrences of this rule's right side will be replaced in the string
        // We increase the use count of each non terminal by one for each time it appears in the right side of the new rule
        // That way all the non-terminal will have a proper usage count
        for (Symbol current : rule) {
            if (current instanceof NonTerminal) {
                ((NonTerminal) current).getRule().useCount++;
            }
        }

        // Iterate through the positions and substitute each occurrence.
        for (int i = 1; i < positions.length; i++) {
            final Rule deepestRule = ruleset.getDeepestRuleAt(positions[i]);
            final int deepestRuleRangeIndex = ruleset.ruleRangeStartIndex(positions[i]);
            RuleLocalIndex ruleLocalIndex = new RuleLocalIndex(deepestRule.id, positions[i] - deepestRuleRangeIndex);

            // Each specific occurrence of a rule in a certain context (represented by ruleLocalIndex) should only be replaced once
            // If this pattern appears in a rule that has been factorized already, then it might still appear multiple times in the original string
            // but only once in the grammar.
            if(!processed.contains(ruleLocalIndex)) {
                final int position = deepestRule.searchTerminalIndex(positions[i] - deepestRuleRangeIndex);
                deepestRule.substitute(rule, position, len);
                processed.add(ruleLocalIndex);
            }
            ruleset.markRange(rule.id, positions[i], positions[i] + len - 1);
        }

        // Put this rule into the map
        //ruleset.getRuleMap().put(rule.id, rule);
    }

    public List<Integer> getCumulativeLength() {
        return Collections.unmodifiableList(cumulativeLength);
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
            return (char) symbols.get(localIndex).value();
        }
    }

    @Override
    public String subSequence(int start, int end) {

        if(end <= start) {
            return "";
        } else if (start + 1 == end) {
            return String.valueOf(charAt(start));
        }

        // Let A -> BBBde, B -> abc
        // [3, 6, 9, 10, 11]
        // Want 4, 8
        // startSymbolIndex = 1;
        // endSymbolIndex =  2;
        final int startSymbolIndex = searchTerminalIndex(start);
        final int endSymbolIndex = searchTerminalIndex(end - 1);
        final int length = end - start;


        // The first and last symbol need to be handled separately,
        // since they can start in the middle of a non-terminal's rule

        final String firstSegment;
        // If it is a terminal then there is no further processing needed
        if (symbols.get(startSymbolIndex) instanceof Terminal terminal) {
            firstSegment = String.valueOf((char) terminal.value());
        } else {
            // If it is a non-terminal the index in the Non-Terminal's rule at which the pattern starts
            // needs to be determined
            final var nonTerminal = (NonTerminal) symbols.get(startSymbolIndex);
            final int lengthBeforeStartSymbol = startSymbolIndex > 0 ? cumulativeLength.get(startSymbolIndex - 1) : 0;
            final int startIndexInStartSymbol = start - lengthBeforeStartSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            firstSegment = nonTerminalRule.subSequence(startIndexInStartSymbol, Math.min(nonTerminalRule.length(), startIndexInStartSymbol + length));
        }

        if (firstSegment.length() == length) return firstSegment;

        final String lastSegment;
        if (symbols.get(endSymbolIndex) instanceof Terminal terminal) {
            lastSegment = String.valueOf((char) terminal.value());
        } else {
            final var nonTerminal = (NonTerminal) symbols.get(endSymbolIndex);
            final int lengthBeforeEndSymbol = endSymbolIndex > 0 ? cumulativeLength.get(endSymbolIndex - 1) : 0;
            final int endIndexInEndSymbol = end - lengthBeforeEndSymbol;

            final var nonTerminalRule = nonTerminal.getRule();
            lastSegment = nonTerminalRule.subSequence(endIndexInEndSymbol, Math.min(nonTerminalRule.length(), endIndexInEndSymbol + length));
        }

        if(firstSegment.length() + lastSegment.length() == length) return  firstSegment + lastSegment;

        final var sb = new StringBuilder(firstSegment);

        IntStream.range(startSymbolIndex + 1, endSymbolIndex)
                .mapToObj(symbols::get)
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
        return symbols.subList(from, to).iterator();
    }

    public Stream<Symbol> streamBetween(int from, int to) {
        return symbols.subList(from, to).stream();
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
