package compression.areacomp;

import compression.utils.AugmentedString;

@FunctionalInterface
public interface AreaFunction {

    /**
     * Calculate an area function for a slice of the lcp array. The value of this function is used to prioritize
     * intervals in the lcp array.
     *
     * @param str The {@link AugmentedString} which contains the relevant data
     * @param low The lower bound for the interval in the lcp array (inclusive)
     * @param high The upper bound for the interval in the lcp array (exclusive)
     * @return An {@link AreaData} object, detailing the area value of this
     */
    AreaData area(AugmentedString str, int low, int high);

    /**
     * A class containing the resulting data from a computation of {@link AreaFunction#area(AugmentedString, int, int)}
     */
    class AreaData implements Comparable<AreaData> {

        /**
         * The lower bound for the interval in the lcp array (inclusive)
         */
        public final int low;

        /**
         * The upper bound for the interval in the lcp array (exclusive)
         */
        public final int high;

        /**
         * The area value of the interval
         */
        public final int area;

        /**
         * The length of the pattern to replace
         */
        public final int len;

        /**
         * Creates a new area data object with the given data
         */
        public AreaData(int low, int high, int area, int len) {
            this.low = low;
            this.high = high;
            this.area = area;
            this.len = len;
        }

        @Override
        public int compareTo(AreaData o) {
            return Integer.compare(this.area, o.area);
        }

        @Override
        public String toString() {
            return String.format("Area[[%d, %d], area: %d, len: %d]", low, high, area, len);
        }
    }
}
