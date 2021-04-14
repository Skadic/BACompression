package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

@SuppressWarnings("Duplicates")
public class LengthFirstArea implements AreaFunction {


    @Override
    public AreaData area(AugmentedString str, int low, int high) {
        var lcpLen = str.lValue(low - 1, high);
        if (lcpLen <= 1) return new AreaData(low, high, 0, 0);
        return new AreaData(low, high, lcpLen, lcpLen);
    }
}
