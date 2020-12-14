package lzss;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The class housing the functions necessary for computing the LZSS Factorization of a string
 * Use the {@link #factorize(String)} function to compute a list of {@link LZFactor} objects,
 * or the {@link #factorizedString(String)} function to get the result as a String.
 */
public class LZ {

    /**
     * Reconstructs a String from {@link LZFactor} objects
     * @param factors An iterable object returning the factors. They must be in the correct order
     * @return The String represented by the factors
     */
    public static String reconstruct(Iterable<LZFactor> factors) {
        // A string builder to hold the string under construction
        var sb = new StringBuilder();

        // Iterate through all the factors
        for (var factor : factors) {
            if(factor.isSingleChar()) {
                // If a factor is a single char, just add it to the string builder and continue with the next factor
                sb.append(factor.getChar());
            } else {
                // If a factor represents a repeated pattern, append as many chars equal to the length of the pattern
                // to the string starting at the starting position of the pattern
                for (int k = 0; k < factor.length(); k++) {
                    sb.append(sb.charAt(factor.patternStartPos() + k));
                }
            }
        }
        // Build the string
        return sb.toString();
    }

    /**
     * Calculates the length of the longest common prefix of two given suffixes in a given string
     *
     * @param s The augmented string which contain the suffixes
     * @param index1 The start index of a suffix in the string
     * @param index2 The start index of a suffix in the string
     * @return The length of the longest common prefix of these two suffixes
     */
    public static int lcpLength(AugmentedString s, int index1, int index2) {

        // An index being -1 represents the suffix being an empty string
        // As a result, the length of the lcp cannot be anything but zero
        if (index1 == -1 || index2 == -1) return 0;

        // If the suffixes are the same, then the longest common prefix they share is the suffix in its entirety
        // Its length then is simply the length of the entire string minus the suffix' start index
        if (index1 == index2) return s.length() - index1;

        // The suffixes' respective index in the suffix array
        int suffixIndex1 = s.suffixArrayIndex(index1);
        int suffixIndex2 = s.suffixArrayIndex(index2);

        int minIndex = Math.min(suffixIndex1, suffixIndex2);
        int maxIndex = Math.max(suffixIndex1, suffixIndex2);

        // The longest common prefix is the minimum of the LCP of all the suffixes between the two indices given
        int min = Integer.MAX_VALUE;

        for (int i = minIndex + 1; i <= maxIndex; i++) {
            min = Math.min(min, s.lcp(i));
            // If the minimum hits 0, there can be no common prefix. In that case, break
            if (min == 0) break;
        }

        return min;
    }

    /**
     * Calculates the LZ-Factor for the index in a string
     *
     * @param s The augmented string to use
     * @param k The index in the string of the suffix to try to factorize
     * @param prevSuffixValue The index in the string of the next lexicographically lesser suffix,
     *                        whose index in the string is still lesser than the given main index
     * @param nextSuffixValue The index in the string of the next lexicographically greater suffix,
     *                        whose index in the string is still lesser than the given main index
     * @return The LZFactor at that index
     */
    public static LZFactor lzFactor(AugmentedString s, int k, int prevSuffixValue, int nextSuffixValue) {

        // Get the length of the longest common prefixes of the suffix at index k and
        // the next lesser and next greater lexicographically respectively
        int lcpLenLesser = lcpLength(s, k, prevSuffixValue);
        int lcpLenGreater = lcpLength(s, k, nextSuffixValue);

        int factorPosition, factorLength;

        // Choose the longer common prefix of the two
        if (lcpLenLesser > lcpLenGreater) {
            factorPosition = prevSuffixValue;
            factorLength = lcpLenLesser;
        } else {
            factorPosition = nextSuffixValue;
            factorLength = lcpLenGreater;
        }

        if (factorLength == 0) {
            // If the factor length is 0, that means it is a single new character
            return new LZFactor(s.charAt(k), k);
        } else {
            // Otherwise, the factor describes a repeating substring
            return new LZFactor(factorPosition, factorLength, k);
        }
    }


    /**
     * Calculates the LZ factorization as a list of LZ factors for a given string
     * @param s A string to factorize
     * @return The LZSS Representation of the string as a list of LZ Factors
     */
    public static List<LZFactor> factorize(String s) {
        // Create an augmented string with the all  data
        final var augmented = new AugmentedString(s);
        final int n = s.length();
        final var list = new ArrayList<LZFactor>();

        int k = 0;
        while (k < n) {
            final int suffixIndex = augmented.suffixArrayIndex(k);
            final int psv = augmented.suffixIndex(augmented.psv(suffixIndex));
            final int nsv = augmented.suffixIndex(augmented.nsv(suffixIndex));
            final LZFactor factor = lzFactor(augmented, k, psv, nsv);
            k = factor.nextIndex();
            list.add(factor);
        }

        return list;
    }

    /**
     * Create a string representing the LZSS Factorization of the given string
     * @param s A string to factorize
     * @return The LZSS Representation of the string
     */
    public static String factorizedString(String s) {
        return factorize(s) // Calculate the LZ Factorization of the string
                .stream()
                .map(LZFactor::toString) // Convert each factors to a string
                .collect(Collectors.joining("")); // Join the separate strings together
    }
}
