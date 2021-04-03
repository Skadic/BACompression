package compression.sequitur;

import java.util.HashMap;

abstract class Symbol {

    protected static final int NUM_TERMINALS = 100000;

    protected Symbol next, prev;
    protected int value;

    protected static HashMap<Symbol, Symbol> DIGRAMS = new HashMap<>();

    public static void join(Symbol left, Symbol right) {

        if (left.next != null) {
            left.deleteDigram();

            if (right.prev != null && right.next != null && right.value == right.prev.value && right.value == right.next.value) {
                DIGRAMS.put(right, right);
            }

            if (left.prev != null && left.value == left.prev.value && left.value == left.next.value) {
                DIGRAMS.put(left.prev, left.prev);
            }
        }

        left.next = right;
        right.prev = left;
    }

    public abstract void cleanUp();

    public void insertAfter(Symbol toInsert) {
        join(toInsert, next);
        join(this, toInsert);
    }

    protected void deleteDigram() {
        if (next.isGuard()) return;

        var dummy = DIGRAMS.get(this);

        if (dummy == this) {
            DIGRAMS.remove(this);
        }
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

    public boolean check() {
        if(next.isGuard()) {
            return false;
        }

        if (!DIGRAMS.containsKey(this)) {
            DIGRAMS.put(this, this);
            return false;
        }

        Symbol found = DIGRAMS.get(this);

        if(found.next != this) {
            match(this, found);
        }

        return true;
    }

    public void substitute(Rule r) {
        cleanUp();
        next.cleanUp();
        prev.insertAfter(new NonTerminal(r));
        if(!prev.check()) {
            prev.next.check();
        }
    }

    private void match(Symbol newD, Symbol matching) {
        Rule r;
        Symbol first, second, dummy;

        if(matching.prev.isGuard() && matching.next.next.isGuard()) {
            // Reuse existing rule

            r = ((Guard) matching.prev).getRule();
            newD.substitute(r);
        } else {
            // Create new rule
            r = new Rule();
            try {
                first = (Symbol) newD.clone();
                second = (Symbol) newD.next.clone();

                r.getGuard().next = first;
                first.prev = r.getGuard();
                first.next = second;
                second.prev = first;
                second.next = r.getGuard();
                r.getGuard().prev = second;

                matching.substitute(r);
                newD.substitute(r);

                DIGRAMS.put(first, first);

            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

        }
        if(r.first().isNonTerminal() && ((NonTerminal) r.first()).getRule().count() == 1) {
            ((NonTerminal) r.first()).expand();
        }

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Symbol sym) {
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

    public static void reset() {
        DIGRAMS.clear();
    }

}
