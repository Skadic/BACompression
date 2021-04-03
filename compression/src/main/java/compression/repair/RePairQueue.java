package compression.repair;

import java.util.*;

class RePairQueue {

    private int size;
    private HashMap<SymbolPair, PairElement> pairs;
    private final List<PairElement> queues;

    /**
     * Creates a new empty queue with a size according to the given length of the sequence this queue is for.
     * The {@link #queues} list has the size ceil(sqrt(<code>sequenceLength</code>))
     * @param sequenceLength The length of the String for which the queue is created
     */
    public RePairQueue(int sequenceLength) {
        final int len = (int) Math.ceil(Math.sqrt(sequenceLength));
        queues = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            queues.add(null);
        }
        pairs = new HashMap<>();
    }

    /**
     * Returns the amount of pairs in this queue
     * @return The amount of pairs
     */
    public int size() {
        return size;
    }

    /**
     * Returns the highest frequency which still has its own queue in {@link #queues}
     * @return The frequency
     */
    private int maxFrequency() {
        return queues.size() - 1;
    }

    /**
     * Adds a {@link SymbolPair} to the queue at the right place. If it exists already, its frequency is just updated
     * @param pair The pair to add to the queue
     */
    public void add(SymbolPair pair) {

        // If the pair is already in the queue, just increase the frequency
        if(contains(pair)) {
            updateFrequency(pair, 1);
            return;
        }

        // Otherwise, insert it
        PairElement element = new PairElement(pair);
        pairs.put(pair, element);
        updateFrequency(pair, 0);
    }

    /**
     * If empty, inserts the element into the index in {@link #queues}, otherwise appends it to the linked list at the given index
     * @param index The index to insert into
     * @param element The element to insert
     */
    private void insertInto(int index, PairElement element) {
        if (queues.get(index) == null) {
            queues.set(index, element);
        } else {
            queues.get(index).append(element);
        }
    }

    public boolean contains(SymbolPair pair) {
        return pairs.containsKey(pair);
    }

    /**
     * Increases or decreses the frequency of a given pair and updates its position in the queue
     * @param pair The {@link SymbolPair} to be updated
     * @param amount The amount *by* witch the frequency should change
     */
    public void updateFrequency(SymbolPair pair, int amount) {
        if(!contains(pair)) return;

        var element = pairs.get(pair);

        if(element.isHead()) {
            var next = element.detachNext();
            int index = indexForFrequency(element.content.frequency());
            // if prevents the queue at given index to be overwritten, if 'element' was not inserted into the queue
            // This happens for elements with initial frequency <= 1, as they are not put into the queue
            if(element == queues.get(index)) {
                queues.set(index, next);
            }
        } else if (element.isTail()) {
            element.detachPrev();
        } else {
            var next = element.detachNext();
            var prev = element.detachPrev();
            prev.connectNext(next);
        }

        // Did the pair fulfil the the requirements for being inserted before?
        var wasInserted = element.content.frequency() > 1;

        element.content.modifyFrequency(amount);

        // Does it now?
        var shouldBeInserted = element.content.frequency() > 1;

        if(shouldBeInserted) {
            insertInto(indexForFrequency(element.content.frequency()), element);
            // If it was not previously inserted, and is now inserted, then the queue grows by 1
            if(!wasInserted) size++;
        } else if(wasInserted) {
            // If it was inserted and should not be inserted again, then the queue shrinks by 1
            size--;
        }
    }

    /**
     * Gets the index at which a pair with the given frequency should be inserted in
     * Usually, pairs are inserted to the index which is equal to their frequency
     * However if said frequency is greater than {@link #maxFrequency()} it is inserted into the list at index zero.
     * @param frequency The frequency to check for
     * @return The index the pair should be inserted into
     */
    private int indexForFrequency(int frequency) {
        return frequency > maxFrequency() ? 0 : frequency;
    }

    /**
     * Removes the first element from a given index and returns the content or null if it is empty.
     * @param index The index to remove from
     * @return The content of the removed element, or null if it was empty
     */
    private SymbolPair removeFirstFrom(int index) {
        if(queues.get(index) == null) {
            return null;
        } else {
            var element = queues.get(index);
            var next = element.detachNext();
            queues.set(index, next);
            return element.content;
        }
    }

    private boolean indexEmpty(int index) {
        return queues.get(index) == null;
    }

    /**
     * Checks whether this queue has no pairs in it
     * @return true, if the queue was empty, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    public SymbolPair getInstance(Symbol left, Symbol right) {
        SymbolPair pair = new SymbolPair(left, right);
        if(pairs.containsKey(pair)) {
            return pairs.get(pair).content;
        }
        return null;
    }

    public SymbolPair getInstance(SymbolPair pair) {
        if(pairs.containsKey(pair)) {
            return pairs.get(pair).content;
        }
        return null;
    }

    /**
     * Gets one of the elements which have the largest amount of occurences. Note that this is approximative to an extent,
     * since all pairs which a frequency of greater than {@link #maxFrequency()} reside in the same list with no special ordering.
     * If there are no such pairs, then the returned symbol pair is definitely one of the pairs with the highest frequency.
     * This method returns null if the queue is empty
     * @return The approximative most frequent pair, or null if the queue is empty
     */
    public SymbolPair poll() {
        if (isEmpty()) {
            return null;
        }
        SymbolPair current = null;

        if(!indexEmpty(0)) current = removeFirstFrom(0);

        for (int i = queues.size() - 1; i >= 0 && current == null; i--) {
            current = removeFirstFrom(i);
        }
        size--;
        pairs.remove(current);

        return current;
    }

    public SymbolPair peek() {
        if (isEmpty()) {
            return null;
        }
        SymbolPair current = null;

        if(!indexEmpty(0)) current = queues.get(0).content;

        for (int i = queues.size() - 1; i >= 0 && current == null; i--) {
            current = Optional.of(queues.get(i)).map(elem -> elem.content).orElse(null);
        }

        return current;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(",\n\t", "[\n\t", "\n]");
        for (int i = 0; i < queues.size(); i++) {
            var row = new StringJoiner(", ", "[", "]");
            PairElement elem = queues.get(i);

            while(elem != null) {
                row.add(elem.content.toString());
                elem = elem.next;
            }

            sj.add((i > 0 ? i : ">" + maxFrequency()) + ": " + row.toString());
        }
        return sj.toString();
    }

    /**
     * A linked-list element for the separate linked-lists in the queue
     */
    private static class PairElement {
        PairElement prev;
        PairElement next;
        SymbolPair content;

        public PairElement(SymbolPair content) {
            this.content = content;
        }

        public boolean isHead() {
            return prev == null;
        }

        public boolean isTail() {
            return next == null;
        }

        public void connectNext(PairElement elem) {
            next = elem;
            if (elem == null) return;
            elem.prev = this;
        }

        public void connectPrev(PairElement elem) {
            prev = elem;
            if (elem == null) return;
            elem.next = this;
        }

        public PairElement detachNext() {
            var temp = next;
            next = null;
            if(temp != null){
                temp.prev = null;
                return temp;
            } else {
                return null;
            }
        }

        public PairElement detachPrev() {
            var temp = prev;
            prev = null;
            if(temp != null){
                temp.next = null;
                return temp;
            } else {
                return null;
            }
        }

        public void append(PairElement pair) {
            if (next != null) {
                next.append(pair);
            } else {
                connectNext(pair);
            }
        }

    }
}
