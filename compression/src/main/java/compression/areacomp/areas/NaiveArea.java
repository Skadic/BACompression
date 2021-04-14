package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

import java.util.Arrays;

public class NaiveArea implements AreaFunction {


    @Override
    public AreaData area(AugmentedString str, int low, int high) {
        // Get the longest common prefix length from the given range in the lcp array
        var lcpLen = Arrays.stream(str.getLcp(), low, high + 1).min().orElse(0);
        // If the length of the longest common prefix is less than or equal to 1, there is no use in replacing it.
        if(lcpLen <= 1) return new AreaData(low, high,0, 0);


        return new AreaData(low, high, lcpLen * (high - low + 1), lcpLen);
    }
}
