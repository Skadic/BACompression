package utils;


import java.util.StringJoiner;

/**
 * A class representing an interval in an array etc.
 */
public record Interval(int start, int end) {

    public int length() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "(", ")").add(String.valueOf(start)).add(String.valueOf(end)).toString();
    }
}
