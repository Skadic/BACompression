package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

public class PotentialCompressionArea implements AreaFunction {
    @Override
    public AreaData area(AugmentedString str, int low, int high) {
        var lcpLen = str.lValue(low - 1, high);
        if (lcpLen <= 1) return new AreaData(low, high, 0, 0);
        var w = high - low + 2;
        return new AreaData(low, high, lcpLen * w - w, lcpLen);
    }
}
