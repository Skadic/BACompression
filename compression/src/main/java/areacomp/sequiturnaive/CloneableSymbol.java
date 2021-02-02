package areacomp.sequiturnaive;

public abstract class CloneableSymbol extends Symbol implements Cloneable {
    @Override
    public CloneableSymbol clone() throws CloneNotSupportedException {
        return (CloneableSymbol) super.clone();
    }
}
