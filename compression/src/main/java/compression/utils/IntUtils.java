package compression.utils;

public class IntUtils {

    private static final int MASK = 0xFFFF_FFFF;

    /**
     * Gets the first n digits of the integer, starting at the most significant bit
     * @param number The number
     * @param n The amount of digits
     * @return The prefix
     */
    public static int bitPrefix(int number, int n) {
        return n >= 32 ? number : number & ((MASK >>> n) ^ MASK);
    }

    /**
     * Gets the bit at the given index, starting at the most significant bit
     * @param number The number
     * @param i The index of the bit
     * @return The bit
     */
    public static int bitAt(int number, int i) {
        return number & (1 << (31 - i));
    }
}
