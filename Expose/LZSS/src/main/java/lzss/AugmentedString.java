package lzss;

import org.javatuples.Pair;
import org.jsuffixarrays.SuffixArrays;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A class that represents a string augmented with a suffix array, an inverse suffix array and an lcp array
 */
public class AugmentedString implements CharSequence, Comparable<String> {

    /**
     * The underlying String to which the data belongs
     */
    public final String underlying;

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
     * The psv array. See {@link #createPsvNsvArrays()} for more details
     */
    private final int[] psv;

    /**
     * The nsv array. See {@link #createPsvNsvArrays()} for more details
     */
    private final int[] nsv;

    /**
     * Creates a new augmented string from the given String and generates the associated data
     * @param s The string to augment
     */
    public AugmentedString(String s) {
        this.underlying = s;
        final var suffixData = SuffixArrays.createWithLCP(s);
        this.suffixArray = suffixData.getSuffixArray();
        this.inverseSuffixArray = inverseSuffixArray(suffixArray);
        this.lcp = suffixData.getLCP();

        // Create the auxiliary arrays
        final var auxiliaries = createPsvNsvArrays();
        this.psv = auxiliaries.getValue0();
        this.nsv = auxiliaries.getValue1();
    }


    /**
     * Gets the i-th substring in lexicographical order
     * @param i The index in the suffix array
     * @return The suffix as a string
     */
    public String suffixString(int i) {
        final int suffix = suffixIndex(i);
        return suffix == -1 ? "" : underlying.substring(suffix);
    }

    /**
     * Gets the index in the string of the i-th suffix lexicographically.
     * If the given index is -1 that is interpreted as the empty suffix and its index in the string is defined as -1.
     * The same is true for when the index is equal to the length of the underlying string (=> out of bounds).
     * This serves to handle boundary conditions in {@link LZ}
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
     * The last element of this array is -1, which is used in algorithms in {@link LZ} to
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
     * Creates the psv and nsv arrays
     *
     * Let k be an index and S_k the suffix that starts at that index and i its index in the suffix array.
     * Then psv[i] contains the suffix array index of the
     * next lexicographically *smaller* suffix that occurs in the string before k.
     * Similarly nsv[i] contains the suffix array index of the
     * next lexicographically *greater* suffix that occurs in the string before k.
     * @return A pair containing the psv and nsv arrays respectively
     */
    private Pair<int[], int[]> createPsvNsvArrays() {
        var psv = new int[length()];
        var nsv = new int[length()];

        // Wordy comments moreso for me to try to explain the algorithm to myself
        for (int i = 0; i <= length(); i++) {
            var j = i - 1;
            // If the i-th suffix in the SA comes before the j-th in the string
            // Observe the suffixes at suffix array indices i and j

            while (suffixIndex(i) < suffixIndex(j)) {
                // if the suffix at SA index i comes before the suffix at SA index j,
                // then that means i is the next lexicographically greater suffix that comes before j
                nsv[j] = i;
                // We can jump to the value psv[j] < j, because all the suffixes in between
                // psv[j] and j cannot come before the j suffix in the string
                j = psv[j];
            }
            // If this while loop concluded then that means that s.suffixIndex(i) > s.suffixIndex(j)
            // That means that the suffix at SA index j comes before the suffix at SA index i in the string
            // Because of the while condition the loop does not loop more often than necessary and as a result
            // the next lexicographically lesser suffix to i which occurs before i's occurrence in the string
            // is the suffix at SA index j

            // In the first iteration with i = 0 and j = -1, psv[0] is set to -1
            // This is because there are no suffixes lexicographically smaller than the first suffix in the SA
            // The if condition serves the purpose of not writing over the array bounds
            // The loop cannot break 1 iteration earlier because necessary computations of the last remaining nsv values
            // in the while loop would be missed
            if (i < psv.length) psv[i] = j;
        }

        return new Pair<>(psv, nsv);
    }

    /**
     * Gets the lcp value at a specific index
     * @param i The index
     * @return The lcp value at that index
     */
    public int lcp(int i) {
        return lcp[i];
    }


    /**
     * Gets the psv value for a given suffix array index i.
     * The psv value is the suffix array index of the next lexicographically greater suffix that occurs in the string
     * before the suffix denoted by the suffix array index i.
     * @param i The suffix array index
     * @return The psv value
     */
    public int psv(int i) {
        return psv[i];
    }

    /**
     * Gets the nsv value for a given suffix array index i.
     * The nsv value is the suffix array index of the next lexicographically lesser suffix that occurs in the string
     * before the suffix denoted by the suffix array index i.
     * @param i The suffix array index
     * @return The nsv value
     */
    public int nsv(int i) {
        return nsv[i];
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
    public String subSequence(int start, int end) {
        return underlying.substring(start, end);
    }

    @Override
    public int compareTo(String o) {
        return underlying.compareTo(o);
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

}

