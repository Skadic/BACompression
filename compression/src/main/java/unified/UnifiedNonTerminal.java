package unified;

import unified.interfaces.UnifiedSymbol;

public class UnifiedNonTerminal implements UnifiedSymbol {

    private final int id;

    public UnifiedNonTerminal(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "R" + id;
    }
}
