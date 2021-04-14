package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

public class DepthWithAddArea implements AreaFunction {


    @Override
    public AreaFunction.AreaData area(AugmentedString str, int low, int high) {
        var lcpLen = str.lValue(low - 1, high);
        if (lcpLen <= 1) return new AreaFunction.AreaData(low, high, 0, 0);

        return new AreaFunction.AreaData(low, high, lcpLen * lcpLen + high - low, lcpLen);
    }
}