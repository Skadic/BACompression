package compression.areacomp.v1;

class Guard extends Symbol {
    private Rule rule;

    public Guard(Rule rule) {
        this.rule = rule;
        this.prev = this;
        this.next = this;
        this.value = '\0';
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
    public int expandedLength() {
        return 0;
    }

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
