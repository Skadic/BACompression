package compression.utils;


import java.util.Objects;
import java.util.StringJoiner;

/**
 * A class representing an interval in an array etc. with start and end inclusive
 */
public record Interval(int start, int end) {

    public int length() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "(", ")").add(String.valueOf(start)).add(String.valueOf(end)).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval interval = (Interval) o;
        return start == interval.start && end == interval.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
