package compression.areacomp.areas;

import compression.areacomp.AreaFunction;
import compression.utils.AugmentedString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DepthWithAddArea implements AreaFunction {

    private List<Integer> buffer = new ArrayList<>();
    private static final int[] EMPTY_POS = new int[0];

    @Override
    public AreaFunction.AreaData area(AugmentedString str, int low, int high) {
        var lcpLen = str.lValue(low - 1, high);
        if (lcpLen <= 1) return new AreaFunction.AreaData(low, high, EMPTY_POS, 0, 0);

        buffer.clear();

        // Remove overlapping occurrences
        Arrays.stream(str.getSuffixArray(), low - 1, high + 1).sorted().forEach(buffer::add);
        for (int i = 1; i < buffer.size(); i++) {
            Integer i1 = buffer.get(i - 1);
            Integer i2 = buffer.get(i);
            if(i2 < i1 + lcpLen) {
                buffer.removeAll(Collections.singletonList(i2));
                i--;
            }
        }

        if (buffer.size() <= 1) return new AreaFunction.AreaData(low, high, EMPTY_POS, 0, 0);
        final int[] validPositions = buffer.stream().mapToInt(Integer::intValue).toArray();

        return new AreaFunction.AreaData(low, high, validPositions, lcpLen * lcpLen + buffer.size(), lcpLen);
    }
}