package repair;

import java.util.Set;

public abstract class Symbol {

    public boolean isPair() {
        return false;
    }

    public boolean isTerminal() {
        return false;
    }

    protected String toStringInternal(boolean shortRep) {
        return toString();
    };

    public abstract Set<SymbolPair> getAllRules();
}
