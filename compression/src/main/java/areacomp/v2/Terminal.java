package areacomp.v2;

class Terminal extends CloneableSymbol implements Cloneable {

    public Terminal(int value) {
        this.value = value;
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
