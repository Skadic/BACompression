package repair;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Terminal extends Symbol implements Cloneable {

    private final int value;

    public Terminal(int value) {
        super();
        this.value = value;
    }


    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf((char) value).replace("\n", "\\n").replace( ' ', '_');
    }

    public int getValue() {
        return value;
    }

    @Override
    public Set<SymbolPair> getAllRules() {
        return new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return value == terminal.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
