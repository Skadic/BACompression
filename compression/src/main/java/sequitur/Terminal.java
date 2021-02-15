package sequitur;

class Terminal extends Symbol implements Cloneable {

    public Terminal(int value) {
        this.value = value;
        this.prev = null;
        this.next = null;
    }

    @Override
    public void cleanUp() {
        join(prev, next);
        deleteDigram();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
