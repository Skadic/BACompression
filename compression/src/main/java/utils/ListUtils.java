package utils;

import java.util.List;

public class ListUtils {

    public static <X> void clearRange(List<X> list, int start, int end) {
        final int len = end - start;
        final int size = list.size();

        for (int i = start; i < size - len; i++) {
            final int copyIndex = len + i;
            final X elem = list.get(copyIndex);
            list.set(i, elem);
        }

        for (int i = size - 1; i >= size - len; i--) {
            list.remove(i);
        }

        list.subList(start, end).clear();
    }
}
