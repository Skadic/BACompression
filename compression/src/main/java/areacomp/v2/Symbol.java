package areacomp.v2;

import java.util.Objects;

abstract class Symbol {

    protected static final int NUM_TERMINALS = 100000;

    protected int value;

    public boolean isNonTerminal() {
        return false;
    }

    public boolean isTerminal() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return value == symbol.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public abstract int expandedLength();

}
