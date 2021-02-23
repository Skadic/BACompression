package areacomp.areas;

import areacomp.AreaFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NaiveArea implements AreaFunction {

    private List<Integer> buffer = new ArrayList<>();

    @Override
    public AreaData area(int[] sa, int[] isa, int[] lcp, int low, int high) {
        // Get the longest common prefix length from the given range in the lcp array
        var lcpLen = Arrays.stream(lcp, low, high + 1).min().orElse(0);
        // If the length of the longest common prefix is less than or equal to 1, there is no use in replacing it.
        if(lcpLen <= 1) return new AreaData(low, high, null, 0, 0);

        buffer.clear();

        // Remove overlapping occurrences
        Arrays.stream(sa, low - 1, high).sorted().forEach(buffer::add);
        for (int i = 1; i < buffer.size(); i++) {
            Integer i1 = buffer.get(i - 1);
            Integer i2 = buffer.get(i);
            if(i2 < i1 + lcpLen) {
                buffer.removeAll(Collections.singletonList(i2));
                i--;
            }
        }

        return new AreaData(low, high, buffer.stream().mapToInt(Integer::intValue).toArray(), lcpLen * buffer.size(), lcpLen);
    }
}
