package utils;


public record Interval(int start, int end) {

    public int length() {
        return end - start + 1;
    }
}
