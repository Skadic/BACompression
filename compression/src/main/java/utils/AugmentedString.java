package utils;

import org.jsuffixarrays.SuffixArrays;

import java.util.Arrays;
import java.util.StringJoiner;

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
    private final int[] lcp;


    /**
     * Creates a new augmented string from the given String and generates the associated data
     * @param s The string to augment
     */
    public AugmentedString(CharSequence s) {
        this.underlying = s;
        final var suffixData = SuffixArrays.createWithLCP(s);
        this.suffixArray = suffixData.getSuffixArray();
        this.inverseSuffixArray = inverseSuffixArray(suffixArray);
        this.lcp = suffixData.getLCP();
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
    public static int[] inverseSuffixArray(int[] suffixArray) {
        var inverse = new int[suffixArray.length];

        for (int i = 0; i < inverse.length - 1; i++) {
            inverse[suffixArray[i]] = i;
        }

        inverse[inverse.length - 1] = -1;

        return inverse;
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
        return underlying.length();
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
                .add("underlying='" + underlying + "'")
                .add("suffixes=" + Arrays.toString(suffixArray))
                .add("inverseSuffixes=" + Arrays.toString(inverseSuffixArray))
                .add("lcp=" + Arrays.toString(lcp))
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

