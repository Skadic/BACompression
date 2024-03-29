package compression.utils;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BucketPred<T> implements IntPredecessor<T>, Iterable<T> {

    private static PrintStream DBG_OUT;
    static {
        try {
            Path path = Path.of("debug", "BucketPred", "log.txt");
            if(!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            DBG_OUT = new PrintStream(Files.newOutputStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private final Deque<Bucket> bucketCache;
    private final Bucket[] bucketsForward;
    private final Bucket[] bucketsBackward;

    private final int bucketSize;
    private final int bucketSizeExponent;
    private final int universeSize;

    private int size;

    private final Int2ObjectMap<T> values;

    @SuppressWarnings("unchecked")
    public BucketPred(int universeSize, int bucketSizeExponent) {
        this.bucketSizeExponent = bucketSizeExponent;
        this.bucketSize = (int) Math.pow(2, bucketSizeExponent);
        this.universeSize = universeSize;
        int bucketCount = (int) Math.ceil(universeSize / (double) bucketSize);
        this.bucketsForward = (Bucket[]) Array.newInstance(Bucket.class, bucketCount);
        this.bucketsBackward = (Bucket[]) Array.newInstance(Bucket.class, bucketCount);
        this.values = new Int2ObjectOpenHashMap<>((int) Math.sqrt(universeSize));
        this.bucketCache = new ArrayDeque<>();
        size = 0;
    }

    public BucketPred(int universeSize) {
        this(universeSize, 9);
    }

    private void checkIndex(int index) {
        if(index < 0 || index >= universeSize) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public T get(int index) {
        checkIndex(index);
        return getValue(index);
    }

    public boolean containsKey(int index) {
        final int bucketIndex = bucketIndex(index);
        return bucketIndexOccupied(bucketIndex) && bucketsForward[bucketIndex].isBitSet(indexInBucket(index));
    }

    private T getValue(int index) {
        int bucketIndex = bucketIndex(index);
        // If there is no bucket here, don't try getting a value from it, as it would be a pointer to a different bucket
        if(!bucketIndexOccupied(bucketIndex)) return null;

        /*Bucket bucket = bucketsForward[bucketIndex];
        return bucket != null ? bucket.get(indexInBucket(index)) : null;*/
        return values.get(index);
    }

    private T insert(int bucketIndex, int indexInBucket, T value) {
        //return bucketsForward[bucketIndex].put(indexInBucket, value);
        bucketsForward[bucketIndex].setBit(indexInBucket, true);
        return values.put(bucketIndex * bucketSize + indexInBucket, value);
    }

    private T delete(int bucketIndex, int indexInBucket) {
        //return bucketsForward[bucketIndex].remove(indexInBucket);
        bucketsForward[bucketIndex].setBit(indexInBucket, false);
        return values.remove(bucketIndex * bucketSize + indexInBucket);
    }

    int counter = 0;
    @Override
    public T put(int index, T value) {
        /*if(++counter % 25000 == 0) {
            DBG_OUT.println(getState());
            counter = 0;
        }*/
        checkIndex(index);
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
    public T remove(int index) {
        checkIndex(index);

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

    /**
     * Gets an empty {@link Bucket}. If there are unused buckets in {@link #bucketCache}, then use one of them instead of allocating
     * a new bucket.
     * @param bucketIndex The index in the bucket array into which the new bucket is supposed to be inserted.
     * @return A new bucket, ready to be inserted into the arrays
     */
    private Bucket retrieveBucket(int bucketIndex) {
        if(bucketCache.isEmpty()) {
            return new Bucket(bucketIndex);
        } else {
            Bucket reused = bucketCache.pop();
            reused.index = bucketIndex;
            return reused;
        }
    }

    /**
     * Delete an unused {@link Bucket} at the given index in the bucket arrays. The bucket is removed from the arrays,
     * and the pointers to the next/previous bucket in {@link #bucketsBackward} and {@link #bucketsForward} are updated.
     * The removed bucket is then inserted into {@link #bucketCache} so it can be reused when a new bucket is needed elsewhere
     * when calling {@link #retrieveBucket(int)}. This serves to reduce unnecessary reallocations.
     * @param bucketIndex The index in the bucket arrays that should be cleared
     */
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

    /**
     * For any index in the data structure this gets the bucket in which the index can be found
     * @param index The index
     * @return The index of the bucket in {@link #bucketsForward} and {@link #bucketsBackward} which contain the given index
     */
    private int bucketIndex(int index) {
        return index >> bucketSizeExponent;
    }

    /**
     * For any index in the data structure this gets the local index in the bucket in which the index can be found
     * @param index The index
     * @return The local index in the bucket which contains the given index
     */
    private int indexInBucket(int index) {
        return index & (bucketSize - 1);
    }

    /**
     * Checks whether the given index is actually occupied by a bucket and not just a pointer to a previous or next bucket
     * @param bucketIndex The index of the bucket in {@link #bucketsForward} and {@link #bucketsBackward}
     * @return true if there is an actual bucket at this index, false if it is just a pointer to a different bucket
     */
    private boolean bucketIndexOccupied(int bucketIndex) {
        return forwardIndexOccupied(bucketIndex);
    }

    /**
     * Checks whether the given index in {@link #bucketsForward} is actually occupied by a bucket and not just a pointer to a previous bucket
     * @param bucketIndex The index of the bucket in {@link #bucketsForward}
     * @return true if there is an actual bucket at this index, false if it is just a pointer to a different bucket
     */
    private boolean forwardIndexOccupied(int bucketIndex) {
        return bucketsForward[bucketIndex] != null && bucketsForward[bucketIndex].index == bucketIndex;
    }

    /**
     * Checks whether the given index in {@link #bucketsBackward} is actually occupied by a bucket and not just a pointer to a next bucket
     * @param bucketIndex The index of the bucket in {@link #bucketsBackward}
     * @return true if there is an actual bucket at this index, false if it is just a pointer to a different bucket
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean backwardIndexOccupied(int bucketIndex) {
        return bucketsBackward[bucketIndex] != null && bucketsBackward[bucketIndex].index == bucketIndex;
    }


    @SuppressWarnings("DuplicatedCode")
    @Override
    public IntEntry<T> floorEntry(int index) {
        //checkIndex(index);
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
        return new IntEntry<>(valueIndex, value);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public IntEntry<T> ceilingEntry(int index) {
        checkIndex(index);
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
        return new IntEntry<>(valueIndex, value);
    }

    /**
     * Returns the greatest value in the data structure whose key is lesser than the given index
     * @param index The index
     * @return The greatest value in the data structure whose key is lesser than to the given index
     */
    @Override
    public IntEntry<T> lowerEntry(int index) {
        checkIndex(index);
        return index > 0 ? floorEntry(index - 1) : null;
    }

    @Override
    public IntEntry<T> higherEntry(int index) {
        checkIndex(index);
        return index > 0 ? ceilingEntry(index + 1) : null;
    }


    @Override
    public Collection<T> values() {
        return valueRange(0, true, universeSize, false);
    }

    @Override
    public Collection<T> valueRange(int from, boolean fromInclusive, int to, boolean toInclusive) {
        List<T> list = new ArrayList<>((int) Math.sqrt(to - from + 1));

        valueRangeIterator(from, fromInclusive, to, toInclusive).forEachRemaining(list::add);

        return list;
    }

    @Override
    public Iterator<T> valueRangeIterator(int from, boolean fromInclusive, int to, boolean toInclusive) {
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
            this(0, true, Integer.MAX_VALUE, true);
        }

        public BucketPredIterator(int min, boolean minInclusive, int max, boolean maxInclusive) {
            this.min = min + (minInclusive ? 0 : 1);
            this.max = max - (maxInclusive ? 0 : 1);

            final int bucketIndex = bucketIndex(this.min);
            this.currentBucket = bucketsBackward[bucketIndex];
            // If this bucket index is not occupied, we need to look for the next one. In that case we start at the beginning of that bucket
            if(bucketIndexOccupied(bucketIndex)) {
                this.nextLocalIndex = indexInBucket(this.min) - 1;
            } else {
                this.nextLocalIndex = -1;
            }
            goToNextSetBit();
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

        private void goToNextSetBit() {
            if(currentBucket == null) {
                this.nextLocalIndex = -1;
                return;
            }

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
        }

        @Override
        public T next() {
            if(!hasNext()) throw new NoSuchElementException();
            final var temp = get(getNextIndex());
            goToNextSetBit();

            return temp;
        }
    }


    private class Bucket {

        private int index;

        private BitSet bits;
        private IntList bitList;
        private final int cutoff = (int) Math.ceil(BucketPred.this.bucketSize / 32D);

        public Bucket(int index) {
            this.index = index;
            this.bits = new BitSet(bucketSize + 1);
            //this.bitList = new IntArrayList(cutoff + 1);
        }

        private boolean bitsetMode() {
            return bits != null;
        }

        public boolean isEmpty() {
            return bits.isEmpty();
        }

        public void setBit(int index, boolean value) {
            if(value) {
                if (bitsetMode()){
                    bits.set(index);
                } else {
                    bitList.add(index);
                    if(size() > cutoff) {
                        rebuild();
                    }
                }
            } else {
                if (bitsetMode()){
                    bits.clear(index);
                    if(size() > cutoff) {
                        rebuild();
                    }
                } else {
                    bitList.rem(index);
                }
            }
        }

        public boolean isBitSet(int index) {
            if(bitsetMode()) {
                return bits.get(index);
            } else {
                return bitList.contains(index);
            }
        }

        @SuppressWarnings("Duplicates")
        public int nextSetBit(int from) {
            if(bitsetMode()) {
                return bits.nextSetBit(from);
            } else {
                int next = -1;
                for (int i = 0, s = bitList.size(); i < s; i++) {
                    int current = bitList.getInt(i);
                    if(current == from) {
                        return from;
                    }
                    if(current >= from && (current < next || next == -1)) {
                        next = current;
                    }
                }

                return next;
            }
        }

        @SuppressWarnings("Duplicates")
        public int previousSetBit(int to) {
            if(bitsetMode()) {
                return bits.previousSetBit(to);
            } else {
                int previous = -1;
                for (int i = 0, s = bitList.size(); i < s; i++) {
                    int current = bitList.getInt(i);
                    if(current == to) {
                        return to;
                    }
                    if(current <= to && (current > previous || previous == -1)) {
                        previous = current;
                    }
                }

                return previous;
            }
        }

        public void rebuild() {
            if(bitsetMode()) {
                bitList = new IntArrayList(BucketPred.this.bucketSize);
                bits.stream().forEach(bitList::add);
                bits = null;
            } else {
                bits = new BitSet(BucketPred.this.bucketSize + 1);
                bitList.forEach((int i) -> bits.set(i));
                bitList = null;
            }
        }

        public int size() {
            if(bitsetMode()) {
                return bits.size();
            } else {
                return bitList.size();
            }
        }
    }

    private static class OccupiedBucketException extends RuntimeException {
        public OccupiedBucketException(int index) {
            super("Index already occupied with Bucket: " + index);
        }
    }

    private static record BucketState(double averageFill, int minimumFill, int maximumFill, int totalAmount, int bucketCount) {
        @Override
        public String toString() {
            return new StringJoiner(", ", BucketState.class.getSimpleName() + "[", "]")
                    .add("averageFill=" + averageFill)
                    .add("minimumFill=" + minimumFill)
                    .add("maximumFill=" + maximumFill)
                    .add("totalAmount=" + totalAmount)
                    .add("bucketCount=" + bucketCount)
                    .toString();
        }
    }

    private BucketState getState() {
        int n = 0;
        int sum = 0;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        Bucket current = bucketsForward[bucketsForward.length - 1];
        while(current != null) {
            final int cardinality = current.size();
            sum += cardinality;
            max = Math.max(max, cardinality);
            min = Math.min(min, cardinality);
            n++;
            current = current.index > 0 ? bucketsForward[current.index - 1] : null;
        }

        return new BucketState(sum / (double) n, min, max, sum, n);
    }
}
