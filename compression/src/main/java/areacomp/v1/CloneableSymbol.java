package areacomp.v1;

abstract class CloneableSymbol extends Symbol implements Cloneable {
    @Override
    public CloneableSymbol clone() throws CloneNotSupportedException {
        return (CloneableSymbol) super.clone();
    }
}
