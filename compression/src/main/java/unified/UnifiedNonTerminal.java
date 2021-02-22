package unified;

import unified.interfaces.UnifiedSymbol;

/**
 * A class representing a non-terminal representing the rule with the given {@link #id}
 */
public record UnifiedNonTerminal(int id) implements UnifiedSymbol {

    @Override
    public String toString() {
        return "R" + id;
    }
}
