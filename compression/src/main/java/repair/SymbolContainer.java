package repair;

import java.util.Objects;

public class SymbolContainer {

    private Symbol symbol;
    private int index;
    private int nextOcc;
    private int prevOcc;

    private SymbolContainer next;
    private SymbolContainer prev;

    public SymbolContainer(Symbol symbol, int index) {
        this.symbol = symbol;
        this.index = index;
        nextOcc = -1;
        prevOcc = -1;
        next = null;
        prev = null;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasPrev() {
        return prev != null;
    }

    public boolean hasNext() {
        return next != null;
    }

    public SymbolContainer next() {
        return next;
    }

    public SymbolContainer prev() {
        return prev;
    }

    public boolean isEmpty() {
        return symbol == null;
    }

    public void connectNext(SymbolContainer next) {
        this.next = next;
        if(next != null) next.prev = this;
    }

    public void connectPred(SymbolContainer prev) {
        this.prev = prev;
        if(prev != null) prev.next = this;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public int getNextOccurence() {
        return nextOcc;
    }

    public int getPrevOccurence() {
        return prevOcc;
    }

    public void setNextOccurence(int nextOcc) {
        this.nextOcc = nextOcc;
    }

    public void setPrevOccurence(int lastOcc) {
        this.prevOcc = lastOcc;
    }

    @Override
    public String toString() {
        return isEmpty() ? "None" : symbol.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolContainer that = (SymbolContainer) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}
