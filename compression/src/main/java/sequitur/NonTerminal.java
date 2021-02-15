package sequitur;

class NonTerminal extends Symbol implements Cloneable {

    private final Rule rule;

    public NonTerminal(Rule rule) {
        this.rule = rule;
        rule.incrementCount();
        value = NUM_TERMINALS + rule.getNumber();

    }

    @Override
    protected Object clone() {
        var sym = new NonTerminal(rule);

        sym.prev = prev;
        sym.next = next;

        return sym;
    }

    @Override
    public void cleanUp() {
        join(prev, next);
        deleteDigram();
        rule.decrementCount();
    }

    @Override
    public boolean isNonTerminal() {
        return true;
    }

    public void expand() {
        join(prev, rule.first());
        join(rule.last(), next);

        DIGRAMS.put(rule.last(), rule.last());

        rule.getGuard().setRule(null);
        rule.setGuard(null);
    }

    public Rule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "R" + rule.getNumber();
    }
}
