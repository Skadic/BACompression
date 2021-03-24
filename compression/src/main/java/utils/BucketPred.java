package utils;

import org.apache.commons.collections4.list.TreeList;

import java.util.*;

public class BucketPred<T> implements Predecessor<Integer, T> {

    private final Bucket[] bucketsForward;
    private final Map<Integer, T> values;

    private final int bucketSize;
    private final int universeSize;

    public BucketPred(int universeSize, int bucketSizeExponent) {
        this.bucketSize = (int) Math.pow(2, bucketSizeExponent);
        this.universeSize = universeSize;
        this.bucketsForward = new Bucket[(int) Math.ceil(universeSize / (double) bucketSize)];
        this.values = new HashMap<>();
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
    public T get(Integer key) {
        checkRange(key);
        return values.get(key);
    }

    @Override
    public T put(Integer index, T value) {
        checkRange(index);
        if(values.containsKey(index)) {
            return values.put(index, value);
        }

        final int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        if(!bucketIndexOccupied(bucketIndex)) {
            newBucket(bucketIndex);
        }

        final Bucket bucket = bucketsForward[bucketIndex];
        bucket.bits.set(localIndex);

        values.put(index, value);

        return null;
    }

    @Override
    public T remove(Integer index) {
        checkRange(index);
        if(!values.containsKey(index)) {
            return null;
        }

        final int bucketIndex = bucketIndex(index);
        final int localIndex = indexInBucket(index);

        final Bucket bucket = bucketsForward[bucketIndex];
        bucket.bits.clear(localIndex);

        if (bucket.bits.isEmpty()) {
            deleteBucket(bucketIndex);
        }

        return values.remove(index);
    }

    private void newBucket(int bucketIndex) {
        if(bucketIndexOccupied(bucketIndex)) throw new OccupiedBucketException(bucketIndex);

        Bucket newBucket = new Bucket(bucketIndex, bucketSize);
        int current = bucketIndex;

        while(current < bucketsForward.length && !bucketIndexOccupied(current)) {
            bucketsForward[current] = newBucket;
            current++;
        }
    }

    private void deleteBucket(int bucketIndex) {
        Bucket replacement = bucketIndex > 0 ? bucketsForward[bucketIndex - 1] : null;

        bucketsForward[bucketIndex] = replacement;
        int current = bucketIndex + 1;
        while (current < bucketsForward.length && !bucketIndexOccupied(bucketIndex)) {
            bucketsForward[current] = replacement;
            current++;
        }
    }

    private int bucketIndex(int index) {
        return index / bucketSize;
    }

    private int indexInBucket(int index) {
        return index % bucketSize;
    }

    private boolean bucketIndexOccupied(int index) {
        return bucketsForward[index] != null && bucketsForward[index].index == index;
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
        int containingBucketLocalIndex;
        if (bucketIndexOccupied(bucketIndex)) {
            containingBucketLocalIndex = bucketsForward[bucketIndex].bits.previousSetBit(localIndex);
            // If no set bit is found
            if (containingBucketLocalIndex == -1) {
                // If the bucket index is not 0 and the previous bucket is either occupied or has a predecessor
                // Get the index of the last set bit in that bucket
                if(bucketIndex > 0 && bucketsForward[bucketIndex - 1] != null) {
                    bucketIndex--;
                    containingBucketLocalIndex = bucketsForward[bucketIndex].bits.previousSetBit(bucketSize);
                    containingBucketIndex = bucketsForward[bucketIndex].index * bucketSize;
                } else {
                    // There is no other element to be found
                    return null;
                }
            }
        } else {
            containingBucketLocalIndex = bucketsForward[bucketIndex].bits.previousSetBit(bucketSize);
        }

        return new Entry<>(containingBucketIndex + containingBucketLocalIndex, values.get(containingBucketIndex + containingBucketLocalIndex));
    }

    @Override
    public Entry<Integer, T> ceilingEntry(Integer index) {
        throw new UnsupportedOperationException("ceilingEntry");
    }

    @Override
    public Entry<Integer, T> higherEntry(Integer key) {
        throw new UnsupportedOperationException("ceilingEntry");
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
        List<T> list = new TreeList<>();
        Entry<Integer, T> current = floorEntry(to);

        while (current != null && (fromInclusive && current.key() >= from || !fromInclusive && current.key() > from)) {
            list.add(0, current.value());
            current = lowerEntry(current.key());
        }

        return list;
    }


    private static class Bucket {

        BitSet bits;
        int index;

        public Bucket(int index, int bucketSize) {
            this.index = index;
            this.bits = new BitSet(bucketSize);
        }
    }


    private static class OccupiedBucketException extends RuntimeException {
        public OccupiedBucketException(int index) {
            super("Index already occupied with Bucket: " + index);
        }
    }
}
