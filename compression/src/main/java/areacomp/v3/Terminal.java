package areacomp.v3;

class Terminal extends Symbol {

    public Terminal(int value) {
        super(value);
    }

    @Override
    public String toString() {
        return String.valueOf((char) value());
    }

}
