package areacomp.v2;

class NonTerminal extends CloneableSymbol implements Cloneable {

    private Rule rule;

    /**
     * Creates a new non-terminal with a pointer to the rule that describes what this non-terminal produces
     * @param rule The rule which this Non-terminal produces
     */
    public NonTerminal(Rule rule) {
        this.rule = rule;
        value = NUM_TERMINALS + rule.getId();
        rule.incrementUseCount();
    }


    @Override
    public boolean isNonTerminal() {
        return true;
    }

    @Override
    public int expandedLength() {
        return rule.length();
    }

    public Rule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "R" + (rule.getId());
    }
}
