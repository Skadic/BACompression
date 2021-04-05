package compression.utils;

import org.jsuffixarrays.SuffixArrays;

import java.util.*;

/**
 * A class that represents a string augmented with a suffix array, an inverse suffix array and an lcp array
 */
public class AugmentedString implements CharSequence {

    /**
     * The underlying String to which the data belongs
     */
    public final CharSequence underlying;

    /**
     * The string's suffix array. An array of the starting indices of the array's suffixes sorted in lexicographical order
     */
    private final int[] suffixArray;

    /**
     * The string's inverse suffix array. An array that maps an index of a suffix in the string itself to its index in the suffix array.
     */
    private final int[] inverseSuffixArray;

    /**
     * The string's lcp array. An array whose entry at index i contains the length of
     * the longest common prefix between suffixArray[i] and suffixArray[i-1].
     * As a result lcp[0] is undefined.
     */
    private int[] lcp;

    /**
     * The child table used to determine the LCP Intervals in accordance with Abouelhoda et al's paper "Optimal Exact String Matching
     * Based on Suffix Arrays"
     */
    private final ChildTableEntry[] childTable;


    /**
     * A static stack used for {@link #childUpDown(int[], ChildTableEntry[])} and {@link #childNextLIndex(int[], ChildTableEntry[])}
     * to prevent repeated allocation of memory
     */
    private static final Deque<Integer> stack = new ArrayDeque<>();

    /**
     * Creates a new augmented string from the given String and generates the associated data
     * @param s The string to augment
     */
    public AugmentedString(CharSequence s) {
        // Workaround for AreaComp V1
        // Since Rules in V1 don't just return their symbols upon calling toString, here a StringBuilder is used,
        // which only uses the CharSequence interface and thus works correctly here.
        this.underlying = new StringBuilder(s).append(Character.MAX_VALUE).toString();


        final var suffixData = SuffixArrays.createWithLCP(underlying);
        this.suffixArray = suffixData.getSuffixArray();
        this.inverseSuffixArray = inverseSuffixArray(suffixArray);
        this.lcp = suffixData.getLCP();
        this.lcp[0] = 0;
        this.childTable = childTable(lcp);
    }


    /**
     * Gets the i-th substring in lexicographical order
     * @param i The index in the suffix array
     * @return The suffix as a string
     */
    public CharSequence suffixSequence(int i) {
        final int suffix = suffixIndex(i);
        return suffix == -1 ? "" : underlying.subSequence(suffix, underlying.length());
    }

    /**
     * Gets the index in the string of the i-th suffix lexicographically.
     * If the given index is -1 that is interpreted as the empty suffix and its index in the string is defined as -1.
     * The same is true for when the index is equal to the length of the underlying string (=> out of bounds).
     * This serves to handle boundary conditions
     * @param i The suffix' index in the suffix array
     * @return The suffix' start index in the string
     */
    public int suffixIndex(int i) {
        // If the given index is -1, this represents the empty string.
        // In that case its starting index in the string is defined as -1
        // Note, that i == underlying.length also returns -1
        // Both of these exist to deal with boundary conditions in algorithms
        if (i == -1) {
            return -1;
        } else {
            return suffixArray[i];
        }
    }

    /**
     * Gets the index in the suffix array for a suffix index in the string
     * If the given index is
     * @param i The start index of the suffix in the string
     * @return The suffix' index in the suffix array
     */
    public int suffixArrayIndex(int i) {
        return inverseSuffixArray[i];
    }

    /**
     * Constructs the inverse suffix array from a given suffix array.
     * This array is actually longer than the underlying string by 1.
     * The last element of this array is -1, which is used to
     * deal with boundary cases
     * @param suffixArray The suffix array to be processed
     * @return The inverse suffix array corresponding to the given suffix array
     */
    private static int[] inverseSuffixArray(int[] suffixArray) {
        var inverse = new int[suffixArray.length];

        for (int i = 0; i < inverse.length - 1; i++) {
            inverse[suffixArray[i]] = i;
        }

        inverse[inverse.length - 1] = -1;

        return inverse;
    }

    private static class ChildTableEntry {
        int up;
        int down;
        int nextLIndex;

        public ChildTableEntry(int up, int down, int nextLIndex) {
            this.up = up;
            this.down = down;
            this.nextLIndex = nextLIndex;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "(", ")")
                    .add(String.valueOf(up))
                    .add(String.valueOf(down))
                    .add(String.valueOf(nextLIndex))
                    .toString();
        }
    }

    /**
     * Calculates the contents of {@link #childTable} with Abouelhoda et al's Algorithm
     * @param lcp The lcp array for which to compute the child table
     * @return The child table
     */
    private static ChildTableEntry[] childTable(int[] lcp) {
        ChildTableEntry[] childTable = new ChildTableEntry[lcp.length];
        for (int i = 0; i < childTable.length; i++) {
            childTable[i] = new ChildTableEntry(-1, -1, -1);
        }

        childUpDown(lcp, childTable);
        childNextLIndex(lcp, childTable);

        return childTable;
    }

    public int up(int i) {
        return i >= 0 && i < childTable.length ? childTable[i].up : -1;
    }

    public int down(int i) {
        return i >= 0 && i < childTable.length ? childTable[i].down : -1;
    }

    public int nextLIndex(int i) {
        return i >= 0 && i < childTable.length ? childTable[i].nextLIndex : -1;
    }

    public boolean hasNextLIndex(int i) {
        return nextLIndex(i) != -1;
    }

    public Collection<Interval> getAllLCPIntervals() {
        return getLCPIntervals(0);
    }

    /**
     * Get all lcp intervals in this string with an l-value of at least minLValue
     * @param minLValue The minimum l value of the lcp interval to include
     * @return A collection of lcp intervals with an l-value of at least minLValue
     */
    public Collection<Interval> getLCPIntervals(int minLValue) {
        var list = new ArrayList<Interval>();

        var lastlIndex = 0;
        var currentlIndex = nextLIndex(0);

        if(0 >= minLValue) list.add(new Interval(lastlIndex, length()));

        while (currentlIndex != -1) {
            if(lValue(lastlIndex, currentlIndex - 1) >= minLValue) list.add(new Interval(lastlIndex, currentlIndex - 1));
            addChildIntervals(lastlIndex, currentlIndex - 1, minLValue, list);

            lastlIndex = currentlIndex;
            currentlIndex = nextLIndex(currentlIndex);
        }

        return list;
    }

    /**
     * Add all the child intervals in the given lcp-interval (low, high) according to Abouelhoda et al to a collection
     *
     * @param low The lower bound of the lcp interval
     * @param high The upper bound of the lcp interval
     * @param intervals The collection to add the intervals into
     */
    public void addChildIntervals(int low, int high, Collection<Interval> intervals) {
        addChildIntervals(low, high, 0, intervals);
    }

    /**
     * Add all the child intervals in the given lcp-interval (low, high) according to Abouelhoda et al with a minimum l-value to a collection
     *
     * @param low The lower bound of the lcp interval
     * @param high The upper bound of the lcp interval
     * @param minLValue The minimum l-value of the returned intervals
     * @param intervals The collection to add the intervals into
     */
    private void addChildIntervals(int low, int high, int minLValue, Collection<Interval> intervals) {
        if (low >= high) return;

        int currentLIndex;

        // Get the first l index of the lcp interval
        if (low < up(high + 1) && up(high + 1) <= high) {
            currentLIndex = up(high + 1);
        } else {
            currentLIndex = down(low);
        }

        // Add the first child interval if its length is greater than 1 and has a high enough l-value
        if(currentLIndex - 1 > low && lValue(low, currentLIndex - 1) >= minLValue) {
            intervals.add(new Interval(low, currentLIndex - 1));
            addChildIntervals(low, currentLIndex - 1, minLValue, intervals);
        }

        while (hasNextLIndex(currentLIndex)) {
            int nextLIndex = nextLIndex(currentLIndex);
            if(nextLIndex - 1 > currentLIndex && lValue(currentLIndex, nextLIndex - 1) >= minLValue) {
                intervals.add(new Interval(currentLIndex, nextLIndex - 1));
                addChildIntervals(currentLIndex, nextLIndex - 1, minLValue, intervals);
            }
            currentLIndex = nextLIndex;
        }

        int lValue = lValue(currentLIndex, high);
        if(high > currentLIndex && lValue >= minLValue) {
            intervals.add(new Interval(currentLIndex, high));
            addChildIntervals(currentLIndex, high, minLValue, intervals);
        }
    }

    /**
     * Returns the lValue of a given lcp interval. Note that this function does not work for arbitrary intervals, but only
     * for lcp intervals as described in Abouelhoda et al's Paper
     * @param low The lower bound of the lcp interval
     * @param high The upper bound of the lcp interval
     * @return The minimum value of the interval [low + 1, high] (inclusively) in {@link #lcp}
     */
    public int lValue(int low, int high) {
        if(low >= high) return 0;
        if(low < up(high + 1) && up(high + 1) <= high) {
            return lcp(up(high + 1));
        } else {
            return lcp(down(low));
        }
    }

    /**
     * Calculates the "up" and "down" fields of {@link #childTable} with Abouelhoda et al's Algorithm
     * @param lcp The lcp array for which to compute the child table
     * @param childTable The child table to populate
     */
    @SuppressWarnings("ConstantConditions")
    private static void childUpDown(int[] lcp, ChildTableEntry[] childTable) {
        int lastIndex = -1;
        stack.push(0);
        for (int i = 1; i < lcp.length; i++) {
            int top = stack.peek();
            while (lcp[i] < lcp[top]) {
                lastIndex = stack.pop();
                top = stack.peek();
                if(lcp[i] <= lcp[top] && lcp[top] != lcp[lastIndex]) {
                    childTable[top].down = lastIndex;
                }
            }

            if (lcp[i] >= lcp[top]) {
                if (lastIndex != -1) {
                    childTable[i].up = lastIndex;
                    lastIndex = -1;
                }
                stack.push(i);
            }
        }
        stack.clear();
    }

    /**
     * Calculates the "nextLIndex" fields of {@link #childTable} with Abouelhoda et al's Algorithm
     * @param lcp The lcp array for which to compute the child table
     * @param childTable The child table to populate
     */
    private static void childNextLIndex(int[] lcp, ChildTableEntry[] childTable) {
        stack.push(0);
        for (int i = 1; i < lcp.length; i++) {
            while(lcp[i] < lcp[stack.peek()]) {
                stack.pop();
            }
            if (lcp[i] == lcp[stack.peek()]) {
                childTable[stack.pop()].nextLIndex = i;
            }
            stack.push(i);
        }
        stack.clear();
    }

    /**
     * Gets the lcp value at a specific index
     * @param i The index
     * @return The lcp value at that index
     */
    public int lcp(int i) {
        return lcp[i];
    }


    @Override
    public int length() {
        return underlying.length() - 1;
    }

    @Override
    public char charAt(int index) {
        return underlying.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return underlying.subSequence(start, end);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AugmentedString.class.getSimpleName() + "[", "]")
                .add("underlying=" + underlying)
                .add("suffixArray=" + Arrays.toString(suffixArray))
                .add("inverseSuffixArray=" + Arrays.toString(inverseSuffixArray))
                .add("lcp=" + Arrays.toString(lcp))
                .add("childTable=" + Arrays.toString(childTable))
                .toString();
    }

    public int[] getSuffixArray() {
        return suffixArray;
    }

    public int[] getInverseSuffixArray() {
        return inverseSuffixArray;
    }

    public int[] getLcp() {
        return lcp;
    }
}

