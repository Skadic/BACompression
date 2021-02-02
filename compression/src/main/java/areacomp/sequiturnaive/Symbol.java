package areacomp.sequiturnaive;

import org.javatuples.Pair;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class Symbol {

    protected static final int NUM_TERMINALS = 100000;

    protected Symbol next, prev;
    protected int value;


    public static void join(Symbol left, Symbol right) {
        if(left != null) {
            left.next = right;
        }

        if(right != null) {
            right.prev = left;
        }
    }

    public abstract void cleanUp();

    public void insertAfter(Symbol toInsert) {
        join(toInsert, next);
        join(this, toInsert);
    }

    public boolean isGuard() {
        return false;
    }

    public boolean isNonTerminal() {
        return false;
    }

    public boolean isTerminal() {
        return false;
    }

    public Symbol detachPred() {
        if (prev == null) return null;

        var temp = prev;
        prev.next = null;
        prev = null;

        return temp;
    }

    public Symbol detachNext() {
        if (next == null) return null;

        var temp = next;
        next.prev = null;
        next = null;

        return temp;
    }


    public Symbol step(int steps) {
        if (steps == 0) return this;

        var current = this;
        for (int i = 0; i < steps && current != null; i++) {
            current = current.next;
        }

        return current;
    }

    /**
     * Substitutes a subsequence with a rule, represented by a non-terminal
     * @param r The rule which should replace this subsequence
     * @param len The length of the subsequence to replace, starting at the current symbol
     * @return The start and end symbol of the replaced sequence
     */
    public SymbolInterval substitute(Rule r, int len) {
        if (len <= 1) return new SymbolInterval(null, null);

        var end = step(len - 1);

        var prev = this.detachPred();
        var next = end.detachNext();

        join(prev, next);

        // When a sequence containing non-terminals is substituted for a rule, it appears once less in the entire grammar
        // Each symbol is checked. If a Non-Terminal is detected, its corresponding rule's use count is decremented
        streamBetween(this, end).forEach(symbol -> {
            if (symbol.isNonTerminal()) {
                ((NonTerminal) symbol).getRule().decrementCount();
            }
        });

        prev.insertAfter(new NonTerminal(r));
        return new SymbolInterval(this, end);
    }

    public Symbol next() {
        return next;
    }

    public Symbol prev() {
        return prev;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Symbol) {
            Symbol sym = (Symbol) o;
            return value == sym.value && next.value == sym.next.value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        long code;

        // Values in linear combination with two
        // prime numbers.

        code = ((21599 * (long) value) + (20507 * (long) next.value));
        code = code % (long) 2265539;
        return (int) code;
    }

    public abstract int expandedLength();

    public static Iterator<Symbol> iteratorBetween(Symbol from, Symbol to) {
        return new SymbolIntervalIterator(from ,to);
    }

    public static Stream<Symbol> streamBetween(Symbol from, Symbol to) {
        return StreamSupport.stream(((Iterable<Symbol>) () -> iteratorBetween(from, to)).spliterator(), false);
    }

    public static SymbolInterval cloneRange(CloneableSymbol from, CloneableSymbol to) throws CloneNotSupportedException {

        Symbol fromNew = from.clone();
        Symbol currentNew = fromNew;
        Symbol currentOld = from.next();
        while (currentOld.prev != to) {
            join(currentNew, (CloneableSymbol) currentOld.clone());
            currentNew = currentNew.next;
            currentOld = currentOld.next();
        }

        return new SymbolInterval(fromNew, currentNew);
    }

    private static class SymbolIntervalIterator implements Iterator<Symbol> {

        private Symbol current;
        private Symbol end;

        public SymbolIntervalIterator(Symbol from, Symbol to) {
            Symbol temp = from;
            while (temp != to) {
                if(temp instanceof Guard) throw new IllegalArgumentException("Symbols not linked");
                temp = temp.next;
            }
            current = from;
            end = to;
        }

        @Override
        public boolean hasNext() {
            return current != null && current.prev != end;
        }

        @Override
        public Symbol next() {
            Symbol temp = current;
            current = current.next;
            return temp;
        }
    }
}
