package unified;

import unified.interfaces.UnifiedSymbol;

public class UnifiedTerminal implements UnifiedSymbol {

    private final char value;


    public UnifiedTerminal(char value) {
        this.value = value;
    }


    public char getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
