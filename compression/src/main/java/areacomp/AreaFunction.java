package areacomp;

@FunctionalInterface
public interface AreaFunction {

    /**
     * Calculate an area function for a slice of the lcp array
     * @param lcp
     * @param low
     * @param hi
     * @return
     */
    int area(int[] sa, int[] isa, int[] lcp, int low, int hi);

}
