package areacomp.v1;

public record SymbolInterval(Symbol start, Symbol end) {

    @Override
    public String toString() {
        var sb = new StringBuilder();

        var current = start;
        while(current != end) {
            sb.append(current.toString()).append(" ");
            current = current.next;
        }
        sb.append(current.toString());


        return sb.toString();
    }
}
