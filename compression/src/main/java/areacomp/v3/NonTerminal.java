package areacomp.v3;

class NonTerminal extends Symbol {

    /**
     * The rule, which this non terminal represents
     */
    private Rule rule;

    /**
     * Creates a new non-terminal with a pointer to the rule that describes what this non-terminal produces
     * @param rule The rule which this Non-terminal produces
     */
    public NonTerminal(Rule rule) {
        super(rule.getId());
        this.rule = rule;
        rule.incrementUseCount();
    }

    /**
     * Get the rule, which is represented by this {@link NonTerminal}
     * @return The rule
     */
    public Rule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "R" + (rule.getId());
    }

}
