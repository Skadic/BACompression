package utils;

import compression.utils.Benchmark;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SparseArrayList<E> extends AbstractList<E> {

    private TreeMap<Integer, Gap> gaps;
    private E[] data;
    private int size;
    private int capacity;
    private int nextIndex;

    @SuppressWarnings("unchecked")
    public SparseArrayList(int capacity) {
        this.data = (E[]) new Object[capacity];
        this.gaps = new TreeMap<>();
        this.size = 0;
        this.capacity = capacity;
        this.nextIndex = 0;
    }

    public SparseArrayList(Collection<E> other) {
        this(other.size());
        addAll(other);
    }

    //
    // 0 0 1 1 1 0 0 1 1 0 0 0 1
    //
    // start length nextIndex
    // 2 3 5
    // 7 2 9
    // 12, 1, 13
    //
    // search 5
    //
    // remaining 5
    // index 0
    // lastStart 0
    //
    // remaining: 5 - (2 - 0) = 3
    // index: 5
    // lastStart: 5
    //
    // remaining: 3 - (7 - 5) = 1
    // index: 9
    // lastStart: 9
    //
    // remaining: 1 - (12 - 9) = -2
    // break
    //
    // return index + remaining;

    protected int actualIndex(int i) {
        var now = System.nanoTime();

        int remaining = i;
        int index = Math.max(0, gaps.getOrDefault(0, Gap.ZERO).nextIndex());
        int lastStart = index;

        var iterator = gaps.values().iterator();
        if (index > 0) iterator.next();
        while (iterator.hasNext()) {
            Gap gap = iterator.next();
            if(remaining - (gap.start - lastStart) < 0) {
                break;
            }
            remaining -= gap.start - lastStart;
            index = gap.nextIndex();
            lastStart = gap.nextIndex();
        }
        Benchmark.updateTime("SAL", "actualIndex", System.nanoTime() - now);
        return index + remaining;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new SparseArrayListIterator();
    }

    @Override
    public boolean add(E e) {
        if(nextIndex == capacity) {
            compact();
        }

        data[nextIndex] = e;
        size++;
        nextIndex++;

        return true;
    }

    @Override
    public E remove(int index) {
        return super.remove(index);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        return super.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return new SparseArrayListIterator();
    }

    @Override
    public void clear() {
        data = (E[]) new Object[capacity];
        size = 0;
        nextIndex = 0;
        gaps.clear();
    }


    @Override
    public E get(int index) {
        if(index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        int actualIndex = actualIndex(index);
        return data[actualIndex];
    }

    @Override
    public E set(int index, E element) {
        int i = actualIndex(index);
        var temp = data[i];
        data[i] = element;
        return temp;
    }

    @Override
    public void add(int index, E element) {
        var actualIndex = actualIndex(index);
        var previousGap = Optional.ofNullable(gaps.floorEntry(actualIndex))
                .map(Map.Entry::getValue)
                .orElse(Gap.ZERO);

        if(previousGap != Gap.ZERO && actualIndex == previousGap.nextIndex() && actualIndex > 0) {
            previousGap.length--;
            data[previousGap.nextIndex()] = element;
            size++;
            return;
        }

        var nextGap = Optional.ofNullable(gaps.ceilingEntry(actualIndex))
                .map(Map.Entry::getValue)
                .orElse(Gap.ZERO);

        if(nextGap != Gap.ZERO) {
            gaps.remove(nextGap.start);
            gaps.put(nextGap.start + 1, new Gap(nextGap.start + 1, nextGap.length - 1));

            for (int i = nextGap.start - 1; i >= actualIndex; i--) {
                data[i + 1] = data[i];
            }

            data[actualIndex] = element;
            size++;
            return;
        }

        if(nextIndex == capacity) {
            compact();
            actualIndex = actualIndex(index);
        }

        for (int i = nextIndex - 1; i >= actualIndex; i--) {
            data[i + 1] = data[i];
        }

        data[actualIndex] = element;
        size++;
        nextIndex++;

    }

    public int capacity() {
        return capacity;
    }

    void compact() {

        E[] copyArray = data;

        if (size == capacity) {
            capacity = Math.max(1, capacity * 2);
            data = (E[]) new Object[capacity];
        }

        int newIndex = 0;
        var nextGap = gaps.ceilingEntry(0);
        int i = nextGap != null && nextGap.getKey() == 0 ? nextGap.getValue().nextIndex() : 0;
        while (i < nextIndex) {
            data[newIndex] = copyArray[i];
            newIndex++;
            if(nextGap != null && nextGap.getValue().start == i) {
                i = nextGap.getValue().nextIndex();
                nextGap = gaps.higherEntry(nextGap.getKey());
            } else {
                i++;
            }
        }
        nextIndex = newIndex;
        gaps.clear();
    }


    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new SparseArraySubList(fromIndex, toIndex);
    }


    private class SparseArraySubList extends AbstractList<E> {

        private final int minBound;
        private final int maxBound;
        private final int actualMinBound;
        private final int actualMaxBound;

        public SparseArraySubList(int minBound, int maxBound) {
            this.minBound = minBound;
            this.maxBound = maxBound;
            this.actualMinBound = actualIndex(minBound);
            this.actualMaxBound = actualIndex(maxBound);
        }

        @Override
        public E get(int index) {
            int actualIndex = actualIndex(index);
            if(minBound <= actualIndex && actualIndex <= maxBound){
                throw new IndexOutOfBoundsException(index);
            }
            return SparseArrayList.this.get(actualMinBound + index);
        }

        @Override
        public int size() {
            return maxBound - minBound;
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public Iterator<E> iterator() {
            return new SparseArraySubListIterator();
        }

        @Override
        public void clear() {
            var nextGap = gaps.higherEntry(actualMinBound);
            while(nextGap != null && nextGap.getKey() < actualMaxBound) {
                gaps.remove(nextGap.getKey());
                nextGap = gaps.higherEntry(actualMinBound);
            }
            gaps.put(actualMinBound, new Gap(actualMinBound, actualMaxBound - actualMinBound));
            SparseArrayList.this.size -= size();
        }

        @NotNull
        @Override
        public ListIterator<E> listIterator() {
            return new SparseArraySubListIterator();
        }

        private class SparseArraySubListIterator extends SparseArrayListIterator {

            public SparseArraySubListIterator() {
                super(minBound, maxBound);
            }

            @Override
            public int nextIndex() {
                return index - actualMinBound;
            }

            @Override
            public int previousIndex() {
                return index - 1 - actualMinBound;
            }
        }
    }

    private static class Gap {

        private static final Gap ZERO = new Gap(0, 0);

        private int start;
        private int length;

        public Gap(int start, int length) {
            this.start = start;
            this.length = length;
        }

        public int nextIndex() {
            return start + length;
        }

        public int start() {
            return start;
        }

        public int length() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    };

    private class SparseArrayListIterator implements ListIterator<E> {

        int index;
        final int min;
        final int max;
        Gap nextGap;
        Gap previousGap;

        public SparseArrayListIterator(int min, int max) {
            int actualMin = actualIndex(min);
            this.index = actualMin;
            this.min = actualMin;
            this.max = actualIndex(max - 1);
            this.nextGap = Optional.ofNullable(gaps.ceilingEntry(actualMin))
                .map(Map.Entry::getValue)
                .orElse(Gap.ZERO);
            this.previousGap = Optional.ofNullable(gaps.floorEntry(actualMin))
                    .map(Map.Entry::getValue)
                    .orElse(Gap.ZERO);
        }

        public SparseArrayListIterator() {
            this(0, size);
        }

        @Override
        public boolean hasNext() {
            return index <= max;
        }

        @Override
        public E next() {
            if(hasNext()) {
                var temp = data[index];
                if(index + 1 == nextGap.start) {
                    index = nextGap.nextIndex();
                    previousGap = nextGap;
                    nextGap = Optional.ofNullable(gaps.ceilingEntry(nextGap.nextIndex()))
                            .map(Map.Entry::getValue)
                            .orElse(Gap.ZERO);
                } else {
                    index++;
                }
                return temp;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasPrevious() {
            return index - 1 >= min;
        }

        @Override
        public E previous() {
            if(hasPrevious()) {
                if(index == previousGap.nextIndex()) {
                    index = previousGap.start - 1;
                    nextGap = previousGap;
                    previousGap = Optional.ofNullable(gaps.lowerEntry(previousGap.start))
                            .map(Map.Entry::getValue)
                            .orElse(Gap.ZERO);
                } else {
                    index--;
                }
                return data[index];
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void set(E e) {
            SparseArrayList.this.set(index, e);
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("add");
        }
    }
}
