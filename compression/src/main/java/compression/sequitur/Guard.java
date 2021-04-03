package compression.sequitur;

class Guard extends Symbol {


    private Rule rule;

    public Guard(Rule rule) {
        this.rule = rule;
        this.prev = this;
        this.next = this;
    }

    @Override
    public void cleanUp() {
        join(prev, next);
    }

    @Override
    public boolean isGuard() {
        return true;
    }

    @Override
    protected void deleteDigram() {
        // Do nothing
    }

    @Override
    public boolean check() {
        return false;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}
