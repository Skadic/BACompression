package lzss;


/**
 * A class representing an LZSS factor.
 * This class can either represent a single character or a repeated character sequence.
 */
public class LZFactor {


    /**
     * The char this factor represents, if it represents a single char
     * If the factor represents a repeated character sequence, this variable contains '\0' as a placeholder
     */
    private final char c;


    /**
     * The starting position of the original pattern. If this factor represents a single character this is equal to the
     * character's occurrence in the string.
     * If it represents a repeating character sequence, this is the start index of the original pattern, referenced by this factor
     */
    private final int patternStartPos;


    /**
     * The length of this pattern. If this factor represents a single character, this is zero.
     * Otherwise, this is the length of the pattern this factor represents
     */
    private final int length;

    /**
     * The index in the string, at which the next factor begins
     */
    private final int nextIndex;

    /**
     * This creates a new LZFactor representing a repeating sequence of characters in a string
     * @param factorStartPos The index in the string which the original pattern starts at, which this factor represents
     * @param length The length of the pattern
     * @param currentIndex The index at which the occurrence of this pattern starts. This is used for calculating the start index of the next factor
     */
    public LZFactor(int factorStartPos, int length, int currentIndex) {
        // This is a placeholder, since this factor does not represent one particular char
        this.c = '\0';
        this.patternStartPos = factorStartPos;
        this.length = length;
        this.nextIndex = currentIndex + length;
    }

    /**
     * This creates a new LZFactor that consists of a single character
     * @param c The character this factor represents
     * @param index The index this character is situated at
     */
    public LZFactor(char c, int index) {
        this.c = c;
        // Since this factor is not pointing to a different character sequence,
        // its pattern start index is equal to its occurrence in the string
        this.patternStartPos = index;
        // The length of a factor representing a single character is zero
        this.length = 0;
        this.nextIndex = index + 1;
    }

    /**
     * Returns the Character which this factor represents.
     * If this factor represents a repeating pattern this will return the null character '\0'
     * @return The char which this character represents, or '\0' if this factor represents a pattern
     */
    public char getChar() {
        return c;
    }

    /**
     * Returns the length of the pattern pointed to by this factor, or zero if it is a single character
     * @return The length of the factor
     */
    public int length() {
        return length;
    }

    /**
     * Gets the start position of the original pattern which this factor represents
     * @return The start position of the original pattern
     */
    public int patternStartPos() {
        return patternStartPos;
    }

    /**
     * Returns whether this pattern represents a single character or a repeating pattern
     * @return true, if the factor represents a single character, false otherwise
     */
    public boolean isSingleChar() {
        return length == 0;
    }

    /**
     * Returns the start index which the factor following this one is situated at
     * @return The start index of the next factor
     */
    public int nextIndex() {
        return nextIndex;
    }

    /**
     * The string representation of a factor that represents a single character is just that character
     * If the Factor represents a repeating pattern, it is a tuple of the original pattern's starting index and its length
     * @return The string representation of the factor
     */
    @Override
    public String toString() {
        return isSingleChar() ? String.valueOf(c) : String.format("(%d,%d)", patternStartPos, length);
    }
}
