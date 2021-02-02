package areacomp.areas;

import areacomp.AreaFunction;

import java.util.Arrays;

public class NaiveArea implements AreaFunction {
    @Override
    public int area(int[] sa, int[] isa, int[] lcp, int low, int hi) {
        // Get the longest common prefix length from the given range in the lcp array
        var lcpLen = Arrays.stream(lcp, low, hi).min().orElse(0);
        // If the length of the longest common prefix is less than or equal to 1, there is no use in replacing it.
        if(lcpLen <= 1) return 0;

        return lcpLen * (hi - low);
    }
}
