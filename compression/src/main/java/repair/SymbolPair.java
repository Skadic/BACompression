package repair;


import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class SymbolPair extends Symbol implements Cloneable {

    private int id;
    private boolean marked = false;

    private Symbol left;
    private Symbol right;

    /**
     * The amount of times this pair appears in the sequence
     */
    private int frequency;

    /**
     * The index of the first occurrence of this pair in the sequence
     */
    private int firstOcc;

    /**
     * The index of the last occurrence of this pair in the sequence
     */
    private int lastOcc;

    /**
     * Next Pair with same frequency
     */
    private SymbolPair freqNext;

    /**
     * Previous Pair with same frequency.
     */
    private SymbolPair freqPrev;

    public SymbolPair(Symbol left, Symbol right) {
        super();
        this.left = left;
        this.right = right;
        frequency = 1;
        firstOcc = -1;
        lastOcc = -1;
    }

    public void mark(int id) {
        if(marked) throw new IllegalStateException("Pair is already marked with an id");
        marked = true;
        this.id = id;
    }

    public int getId() {
        if(!marked) throw new IllegalStateException("Tried to get id without pair being marked");
        return id;
    }

    public Symbol getLeft() {
        return left;
    }

    public Symbol getRight() {
        return right;
    }

    public void setFirstOccurrence(int firstOcc) {
        this.firstOcc = firstOcc;
    }

    public void setLastOccurrence(int lastOcc) {
        this.lastOcc = lastOcc;
    }

    public int getFirstOccurrence() {
        return firstOcc;
    }

    public int getLastOccurrence() {
        return lastOcc;
    }

    public int frequency() {
        return frequency;
    }

    public void modifyFrequency(int amount) {
        frequency += amount;
    }

    @Override
    public Set<SymbolPair> getAllRules() {
        var leftSet = left.getAllRules();
        var rightSet = right.getAllRules();
        leftSet.add(this);
        leftSet.addAll(rightSet);

        return leftSet;
    }

    @Override
    public boolean isPair() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolPair that = (SymbolPair) o;
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return toStringInternal(false);
    }

    @Override
    protected String toStringInternal(boolean shortRep) {
        if(!marked) {
            var leftStr = left.toStringInternal(true);
            var rightStr = right.toStringInternal(true);
            return new StringJoiner(", ", "[", "]").add(leftStr).add(rightStr).toString();
        } else {
            if (shortRep) {
                return "R" + id;
            } else {
                var leftStr = left.toStringInternal(true);
                var rightStr = right.toStringInternal(true);
                return "R" + id + ": " + new StringJoiner(", ", "[", "]").add(leftStr).add(rightStr).toString();
            }
        }
    }
}
