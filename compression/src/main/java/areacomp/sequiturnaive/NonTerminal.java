package areacomp.sequiturnaive;

public class NonTerminal extends CloneableSymbol implements Cloneable {

    private Rule rule;

    /**
     * Creates a new non-terminal with a pointer to the rule that describes what this non-terminal produces
     * @param rule The rule which this Non-terminal produces
     */
    public NonTerminal(Rule rule) {
        this.rule = rule;
        value = NUM_TERMINALS + rule.getNumber();
        rule.incrementCount();
    }

    /**
     * Clones only this Symbol, and not any connected symbols
     * @return A clone of this symbol, which its {@link #prev} and {@link #next} fields still pointing to their original targets
     */
    @Override
    public CloneableSymbol clone() {
        var sym = new NonTerminal(rule);

        sym.prev = prev;
        sym.next = next;

        return sym;
    }

    /**
     * Expands this non-terminal according to its {@link #rule}
     */
    public void expand() {
        try {
            var interval = Symbol.cloneRange((CloneableSymbol)rule.first(), (CloneableSymbol) rule.last());

            join(prev, interval.start());
            join(interval.end(), next);

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void cleanUp() {
        join(prev, next);
        rule.decrementCount();
    }

    @Override
    public boolean isNonTerminal() {
        return true;
    }

    @Override
    public int expandedLength() {
        return rule.expandedLength();
    }


    public Rule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "R" + (rule.getNumber());
    }
}
