package compression.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

public class BucketPred<T> implements Predecessor<Integer, T>, Iterable<T> {


    private final Deque<Bucket> bucketCache;
    private final Bucket[] bucketsForward;
    private final Bucket[] bucketsBackward;

    private final int bucketSize;
    private final int universeSize;

    private int size;

    @SuppressWarnings("unchecked")
    public BucketPred(int universeSize, int bucketSizeExponent) {
        this.bucketSize = (int) Math.pow(2, bucketSizeExponent);
        this.universeSize = universeSize;
        int bucketCount = (int) Math.ceil(universeSize / (double) bucketSize);
        this.bucketsForward = (Bucket[]) Array.newInstance(Bucket.class, bucketCount);
        this.bucketsBackward = (Bucket[]) Array.newInstance(Bucket.class, bucketCount);
        this.bucketCache = new ArrayDeque<>();
        size = 0;
    }

    public BucketPred(int universeSize) {
        this(universeSize, 9);
    }

    private void checkRange(int index) {
        if(index < 0 || universeSize <= index) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public T get(Integer index) {
        checkRange(index);
        Bucket bucket = bucketsForward[bucketIndex(index)];
        return bucket != null ? bucket.get(indexInBucket(index)) : null;
    }

    public boolean containsKey(Integer index) {
        return get(index) != null;
    }

    private T insert(int bucketIndex, int indexInBucket, T value) {
        return bucketsForward[bucketIndex].put(indexInBucket, value);
    }

    private T delete(int bucketIndex, int indexInBucket) {
        return bucketsForward[bucketIndex].remove(indexInBucket);
    }

    @Override
    public T put(Integer index, T value) {
        checkRange(index);
        Objects.requireNonNull(value);

        final int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        if(containsKey(index)) {
            return insert(bucketIndex, localIndex, value);
        }

        if(!bucketIndexOccupied(bucketIndex)) {
            newBucket(bucketIndex);
        }

        insert(bucketIndex, localIndex, value);
        size++;

        return null;
    }

    @Override
    public T remove(Integer index) {
        checkRange(index);

        final int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        var removed = delete(bucketIndex, localIndex);
        if(removed == null) {
            return null;
        }


        final Bucket bucket = bucketsForward[bucketIndex];
        if (bucket.isEmpty()) {
            deleteBucket(bucketIndex);
        }
        size--;
        return removed;
    }

    private void newBucket(int bucketIndex) {
        if(bucketIndexOccupied(bucketIndex)) throw new OccupiedBucketException(bucketIndex);

        Bucket newBucket = retrieveBucket(bucketIndex);
        int current = bucketIndex;

        while(current < bucketsForward.length && !forwardIndexOccupied(current)) {
            bucketsForward[current] = newBucket;
            current++;
        }

        current = bucketIndex;
        while (current >= 0 && !backwardIndexOccupied(current)) {
            bucketsBackward[current] = newBucket;
            current--;
        }
    }

    private Bucket retrieveBucket(int bucketIndex) {
        if(bucketCache.isEmpty()) {
            return new Bucket(bucketIndex, bucketSize);
        } else {
            Bucket reused = bucketCache.pop();
            reused.index = bucketIndex;
            return reused;
        }
    }

    private void deleteBucket(int bucketIndex) {
        int current;

        if(!bucketIndexOccupied(bucketIndex)) return;

        bucketCache.push(bucketsForward[bucketIndex]);

        // Replace pointers in the forward referencing Bucket Array
        Bucket forwardReplacement = bucketIndex > 0 ? bucketsForward[bucketIndex - 1] : null;
        bucketsForward[bucketIndex] = forwardReplacement;
        current = bucketIndex + 1;
        while (current < bucketsForward.length && !forwardIndexOccupied(bucketIndex)) {
            bucketsForward[current] = forwardReplacement;
            current++;
        }

        // Replace pointers in the backward referencing Bucket Array
        Bucket backwardReplacement = bucketIndex < bucketsBackward.length - 1 ? bucketsBackward[bucketIndex + 1] : null;
        bucketsBackward[bucketIndex] = backwardReplacement;
        current = bucketIndex - 1;
        while (current < bucketsForward.length && !backwardIndexOccupied(bucketIndex)) {
            bucketsBackward[current] = backwardReplacement;
            current--;
        }
    }

    private int bucketIndex(int index) {
        return index / bucketSize;
    }

    private int indexInBucket(int index) {
        return index % bucketSize;
    }

    private boolean bucketIndexOccupied(int bucketIndex) {
        return forwardIndexOccupied(bucketIndex) && backwardIndexOccupied(bucketIndex);
    }

    private boolean forwardIndexOccupied(int bucketIndex) {
        return bucketsForward[bucketIndex] != null && bucketsForward[bucketIndex].index == bucketIndex;
    }

    private boolean backwardIndexOccupied(int bucketIndex) {
        return bucketsBackward[bucketIndex] != null && bucketsBackward[bucketIndex].index == bucketIndex;
    }

    @Override
    public Entry<Integer, T> floorEntry(Integer index) {
        checkRange(index);
        int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        if(bucketsForward[bucketIndex] == null) {
            return null;
        }

        // In case this bucketIndex is not occupied, the total index of the actual bucket is different
        int containingBucketIndex = bucketsForward[bucketIndex].index * bucketSize;
        // The index inside the bucket at which the desired element lies
        int containingBucketLocalIndex;
        if (bucketIndexOccupied(bucketIndex)) {
            containingBucketLocalIndex = bucketsForward[bucketIndex].previousSetBit(localIndex);
            // If no set bit is found
            if (containingBucketLocalIndex == -1) {
                // If the bucket index is not 0 and the previous bucket is either occupied or has a predecessor
                // Get the index of the last set bit in that bucket
                if(bucketIndex > 0 && bucketsForward[bucketIndex - 1] != null) {
                    bucketIndex--;
                    containingBucketLocalIndex = bucketsForward[bucketIndex].previousSetBit(bucketSize);
                    containingBucketIndex = bucketsForward[bucketIndex].index * bucketSize;
                } else {
                    // There is no other element to be found
                    return null;
                }
            }
        } else {
            containingBucketLocalIndex = bucketsForward[bucketIndex].previousSetBit(bucketSize);
        }

        int valueIndex = containingBucketIndex + containingBucketLocalIndex;
        T value = get(valueIndex);
        return new Entry<>(valueIndex, value);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Entry<Integer, T> ceilingEntry(Integer index) {
        checkRange(index);
        int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        if(bucketsBackward[bucketIndex] == null) {
            return null;
        }

        // In case this bucketIndex is not occupied, the total index of the actual bucket is different
        int containingBucketIndex = bucketsBackward[bucketIndex].index * bucketSize;
        // The index inside the bucket at which the desired element lies
        int containingBucketLocalIndex;
        if (bucketIndexOccupied(bucketIndex)) {
            containingBucketLocalIndex = bucketsBackward[bucketIndex].nextSetBit(localIndex);
            // If no set bit is found
            if (containingBucketLocalIndex == -1) {
                // If the bucket is not at the last possible index and the next bucket is either occupied or has a successor
                // Get the index of the first set bit in that bucket
                if(bucketIndex < bucketsBackward.length - 1 && bucketsBackward[bucketIndex + 1] != null) {
                    bucketIndex++;
                    containingBucketLocalIndex = bucketsBackward[bucketIndex].nextSetBit(0);
                    containingBucketIndex = bucketsBackward[bucketIndex].index * bucketSize;
                } else {
                    // There is no other element to be found
                    return null;
                }
            }
        } else {
            containingBucketLocalIndex = bucketsBackward[bucketIndex].nextSetBit(0);
        }

        int valueIndex = containingBucketIndex + containingBucketLocalIndex;
        T value = get(valueIndex);
        return new Entry<>(valueIndex, value);
    }

    @Override
    public Entry<Integer, T> higherEntry(Integer key) {
        return ceilingEntry(key + 1);
    }

    @Override
    public Entry<Integer, T> lowerEntry(Integer key) {
        checkRange(key);
        return key > 0 ? floorEntry(key - 1) : null;
    }

    @Override
    public Collection<T> values() {
        return valueRange(0, true, universeSize, false);
    }

    @Override
    public Collection<T> valueRange(Integer from, boolean fromInclusive, Integer to, boolean toInclusive) {
        if(!toInclusive) to--;
        List<T> list = new ArrayList<>((int) Math.sqrt(to - from + 1));
        Entry<Integer, T> current = floorEntry(to);

        while (current != null && (fromInclusive && current.key() >= from || !fromInclusive && current.key() > from)) {
            list.add(0, current.value());
            current = lowerEntry(current.key());
        }

        return list;
    }

    @Override
    public Iterator<T> valueRangeIterator(Integer from, boolean fromInclusive, Integer to, boolean toInclusive) {
        return new BucketPredIterator(from, fromInclusive, to, toInclusive);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new BucketPredIterator();
    }

    public int size() {
        return size;
    }

    private class BucketPredIterator implements Iterator<T> {

        final int min;
        final int max;
        Bucket currentBucket;
        int nextLocalIndex;
        final int expectedSize;

        public BucketPredIterator() {
            this(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true);
        }

        public BucketPredIterator(int min, boolean minInclusive, int max, boolean maxInclusive) {
            this.min = min + (minInclusive ? 0 : 1);
            this.max = max - (maxInclusive ? 0 : 1);

            final int bucketIndex = bucketIndex(this.min);
            this.currentBucket = bucketsBackward[bucketIndex];
            // If this bucket index is not occupied, we need to look for the next one. In that case we start at the beginning of that bucket
            if(bucketIndexOccupied(bucketIndex)) {
                this.nextLocalIndex = indexInBucket(this.min);
            } else if (currentBucket != null) {
                this.nextLocalIndex = currentBucket.nextSetBit(0);
            } else {
                this.nextLocalIndex = -1;
            }
            this.expectedSize = size();
        }

        private int getNextIndex() {
            return currentBucket.index * bucketSize + nextLocalIndex;
        }

        private void checkForModification() {
            if(size() != expectedSize) throw new ConcurrentModificationException();
        }

        @Override
        public boolean hasNext() {
            checkForModification();
            return currentBucket != null && getNextIndex() <= max;
        }

        @Override
        public T next() {
            if(!hasNext()) throw new NoSuchElementException();
            final var temp = get(getNextIndex());
            // Advance to the next set bit in the bucket
            this.nextLocalIndex = currentBucket.nextSetBit(this.nextLocalIndex + 1);
            // If there is no next set bit...
            if (this.nextLocalIndex == -1) {
                // Go to the next bucket if there is potentially a next bucket
                this.currentBucket = this.currentBucket.index < bucketsBackward.length - 1 ?
                        bucketsBackward[currentBucket.index + 1] :
                        null;
                // If there actually is a next bucket
                if(this.currentBucket != null) {
                    // Set the next local index to the first set bit of the new bucket
                    this.nextLocalIndex = currentBucket.nextSetBit(0);
                }
            }
            return temp;
        }
    }

    @SuppressWarnings("unchecked")
    private class Bucket {

        BitSet bits;
        int index;
        Object[] data;

        public Bucket(int index, int bucketSize) {
            this.index = index;
            this.bits = new BitSet(bucketSize + 1);
            this.data = new Object[bucketSize];
        }


        public T put(int localIndex, T value) {
            final T temp = (T) data[localIndex];
            data[localIndex] = value;
            bits.set(localIndex);
            return temp;
        }

        public T remove(int localIndex) {
            final T temp = (T) data[localIndex];
            data[localIndex] = null;
            bits.clear(localIndex);
            return temp;
        }

        public T get(int localIndex) {
            return (T) data[localIndex];
        }

        public boolean isEmpty() {
            return bits.isEmpty();
        }

        public int nextSetBit(int from) {
            return bits.nextSetBit(from);
        }

        public int previousSetBit(int to) {
            return bits.previousSetBit(to);
        }
    }

    private static class OccupiedBucketException extends RuntimeException {
        public OccupiedBucketException(int index) {
            super("Index already occupied with Bucket: " + index);
        }
    }
}
