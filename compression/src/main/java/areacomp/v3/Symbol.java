package areacomp.v3;

import java.util.Objects;

abstract class Symbol {

    private final int value;

    protected Symbol(int value) {
        this.value = value;
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

    public int value() {
        return value;
    }

    public abstract int terminalLength();
}
