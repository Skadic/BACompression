package areacomp.v1;

class Terminal extends CloneableSymbol implements Cloneable {

    public Terminal(int value) {
        this.value = value;
        this.prev = null;
        this.next = null;
    }

    @Override
    public CloneableSymbol clone() {
        var sym = new Terminal(value);

        sym.prev = prev;
        sym.next = next;

        return sym;
    }

    @Override
    public void cleanUp() {
        join(prev, next);
        //deleteDigram();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf((char) value);
    }

    @Override
    public int expandedLength() {
        return 1;
    }
}
