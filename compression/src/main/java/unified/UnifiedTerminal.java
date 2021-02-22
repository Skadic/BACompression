package unified;

import unified.interfaces.UnifiedSymbol;

/**
 * A class representing a terminal character
 */
public record UnifiedTerminal(char value) implements UnifiedSymbol {

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
