package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

public class HeightAdvantageArea implements AreaFunction {


    @Override
    public AreaFunction.AreaData area(AugmentedString str, int low, int high) {
        var lcpLen = str.lValue(low - 1, high);
        if (lcpLen <= 1) return new AreaFunction.AreaData(low, high, 0, 0);

        return new AreaFunction.AreaData(low, high,  10 * lcpLen + Math.min(9, (int) Math.log(high - low + 1)), lcpLen);
    }
}