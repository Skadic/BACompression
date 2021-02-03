package areacomp.v1;

public abstract class CloneableSymbol extends Symbol implements Cloneable {
    @Override
    public CloneableSymbol clone() throws CloneNotSupportedException {
        return (CloneableSymbol) super.clone();
    }
}
