package utils;


import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public class XFastTrie<T> implements Predecessor<Integer, T> {

    private final Map<Integer, Node>[] lss;
    private int size;

    public XFastTrie() {
        this(50);
    }

    @SuppressWarnings("unchecked")
    public XFastTrie(int expectedSize) {
        this.lss = new Map[33];
        for (int i = 0; i < lss.length; i++) {
            lss[i] = new HashMap<>(Math.min((int) Math.pow(2, i), expectedSize));
        }
        size = 0;
    }

    @Override
    public T put(Integer key, T value) {
        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "put");
        var prevContent = lss[0].get(key);
        if(prevContent != null) {
            var temp = prevContent.content;
            prevContent.content = value;
            Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "put");
            return temp;
        }

        Node newNode = new Node(key, value);

        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "put link");
        linkNode(newNode);
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "put link");

        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "put insert");
        insertNode(newNode);
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "put insert");

        size++;
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "put");
        return null;
    }

    /**
     * Inserts a node into the linked list structure between the nodes in {@link #lss}[0].
     * This links the node with the next-smaller node as the given node's predecessor and the one with the next-greater
     * key as the than the
     * @param newNode The new node to insert
     */
    private void linkNode(Node newNode) {
        final var key = newNode.key;

        final var floorCeilingNodes = floorCeilingNode(key);
        if(floorCeilingNodes == null) return;

        final Node predecessor = floorCeilingNodes.getValue0();
        final Node successor = floorCeilingNodes.getValue1();

        if(predecessor != null && predecessor.key > newNode.key) {
            throw new IllegalStateException(String.format("Predecessor %s greater than new node %s%n", predecessor.toString(), newNode.toString()));
        }

        if(successor != null && successor.key < newNode.key) {
            throw new IllegalStateException(String.format("Successor %s lesser than new node %s%n", successor.toString(), newNode.toString()));
        }

        newNode.connectPred(predecessor);
        newNode.connectSucc(successor);

    }

    /**
     * Inserts a node into the data structure. This adds the node itself, as well as the required ancestors
     * @param newNode The new node to insert
     */
    private void insertNode(Node newNode) {
        final var key = newNode.key;
        lss[0].put(key, newNode);

        int bitPrefixLength = 32 - 1;
        int bitPrefix = IntUtils.bitPrefix(key, bitPrefixLength);

        // The current node. Initialized as the new node
        Node current = newNode;
        // The current node's ancestor (one level further up). Initialized as this node's direct ancestor, if it exists
        Node currentAncestor = lss[1].get(bitPrefix);

        for (int i = 1; i < lss.length; i++) {
            if(currentAncestor == null) {
                currentAncestor = new Node(bitPrefix, i);
                putNewAncestor(currentAncestor, current, newNode);
            } else {
                if(current.isChild0()) {
                    currentAncestor.connectChild0(current, false);
                    if (currentAncestor.child1.key < newNode.key) {
                        // Descendant Pointer
                        currentAncestor.connectChild1(newNode, true);
                    }
                } else if (current.isChild1()) {
                    currentAncestor.connectChild1(current, false);
                    if(currentAncestor.child0.key > newNode.key) {
                        // Descendant Pointer
                        currentAncestor.connectChild0(newNode, true);
                    }
                }
            }

            // So that lss[i + 1] does not fail
            if(i + 1 >= lss.length) {
                break;
            }

            bitPrefixLength = 32 - i - 1;
            bitPrefix = IntUtils.bitPrefix(key, bitPrefixLength);
            current = currentAncestor;
            currentAncestor = lss[i + 1].get(bitPrefix);
        }
    }

    /**
     * Inserts the ancestor node in its respective level in {@link #lss}, links it with its descendant and sets up the
     * descendant pointers to the corresponding leaf node. This is used when a new leaf is inserted and new ancestors must be created.
     * @param ancestor The new ancestor to be inserted
     * @param descendant The direct descendant of that ancestor
     * @param newNode The newly created leaf node to which this new ancestor belongs
     */
    private void putNewAncestor(Node ancestor, Node descendant, Node newNode) {
        lss[ancestor.level].put(ancestor.key, ancestor);
        if(descendant.isChild0()) {
            ancestor.connectChild0(descendant, false);
            // Descendant pointer
            ancestor.connectChild1(newNode, true);
        } else if (descendant.isChild1()) {
            // Descendant pointer
            ancestor.connectChild0(newNode, true);
            ancestor.connectChild1(descendant, false);
        }
    }

    @Override
    public T remove(Integer key) {
        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "remove");
        final var removed = lss[0].get(key);

        if(removed == null) {
            Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "remove");
            return null;
        }

        final var pred = removed.pred;
        final var succ = removed.succ;
        if(pred != null) {
            pred.connectSucc(succ);
        } else if (succ != null) {
            succ.connectPred(null);
        }

        // If at any point a node is found that has another child tree, the parent nodes may not be deleted
        boolean forbidDeletion = false;

        for (int i = 1; i < lss.length; i++) {
            final int bitPrefixLength = 32 - i;
            final int bitPrefix = IntUtils.bitPrefix(key, bitPrefixLength);

            // The current node's ancestor (one level further up)
            Node currentAncestor = lss[i].get(bitPrefix);
            // The current node. If the bit after the current ancestor's key is 0, then take its child 0, else child 1
            Node current = IntUtils.bitAt(key, bitPrefixLength) == 0 ? currentAncestor.child0 : currentAncestor.child1;

            if(current.isChild0()) {
                if(!currentAncestor.hasChild1()) {
                    if(!forbidDeletion) {
                        lss[i].remove(bitPrefix);
                    } else if (currentAncestor.child1 == removed) {
                        currentAncestor.connectChild1(pred, true);
                    }
                } else {
                    currentAncestor.connectChild0(succ, true);
                    forbidDeletion = true;
                }
            } else {
                if(!currentAncestor.hasChild0()) {
                    if(!forbidDeletion) {
                        lss[i].remove(bitPrefix);
                    } else if (currentAncestor.child0 == removed) {
                        currentAncestor.connectChild0(succ, true);
                    }
                } else {
                    currentAncestor.connectChild1(pred, true);
                    forbidDeletion = true;
                }
            }
        }
        size--;
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "remove");
        return lss[0].remove(key).content;
    }



    @Override
    public T get(Integer key) {
        return getOrDefault(key, null);
    }

    @Override
    public T getOrDefault(Integer key, T def) {
        var node = lss[0].get(key);
        return node != null ? node.content : def;
    }

    @Override
    public Entry<Integer, T> floorEntry(Integer key) {
        var node = floorNode(key);
        return node != null ? new Entry<>(node.key, node.content) : null;
    }

    @Override
    public Entry<Integer, T> ceilingEntry(Integer key) {
        var node = ceilingNode(key);
        return node != null ? new Entry<>(node.key, node.content) : null;
    }

    @Override
    public Entry<Integer, T> lowerEntry(Integer key) {
        var node = floorNode(key - 1);
        return node != null ? new Entry<>(node.key, node.content) : null;
    }

    @Override
    public Entry<Integer, T> higherEntry(Integer key) {
        var node = ceilingNode(key + 1);
        return node != null ? new Entry<>(node.key, node.content) : null;
    }

    public Integer floorKey(int key) {
        return floorEntry(key).key();
    }

    public Integer ceilingKey(int key) {
        return ceilingEntry(key).key();
    }

    public Integer lowerKey(int key) {
        return lowerEntry(key).key();
    }

    public Integer higherKey(int key) {
        return higherEntry(key).key();
    }



    private Node searchLowestAncestor(int key) {
        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "lowest ancestor");
        int top = lss.length - 1;
        int bottom = 0;
        Node lastFind = null;
        while(top > bottom) {
            final int mid = (top + bottom) / 2;
            final int bitPrefixLength = 32 - mid;
            final int bitPrefix = IntUtils.bitPrefix(key, bitPrefixLength);

            final Node find = lss[mid].get(bitPrefix);
            if(find != null) {
                lastFind = find;
                top = mid;
            } else {
                bottom = mid + 1;
            }
        }
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "lowest ancestor");
        return lastFind;
    }

    @Nullable
    private Node floorNode(int key) {
        Benchmark.startTimer(XFastTrie.class.getSimpleName(), "floorNode");
        final var lowestAncestor = searchLowestAncestor(key);
        Benchmark.stopTimer(XFastTrie.class.getSimpleName(), "floorNode");
        if(lowestAncestor == null) {
            return null;
        } else if (lowestAncestor.isLeaf()){
            // If the lowestAncestor is a leaf, then that means the Trie contains the key itself.
            // So return the predecessor
            return lowestAncestor;
        } else if(lowestAncestor.hasChild0()) {
            return lowestAncestor.child1;
        } else {
            return lowestAncestor.child0.pred;
        }
    }

    private Node ceilingNode(int key) {
        final var lowestAncestor = searchLowestAncestor(key);

        if(lowestAncestor == null) {
            return null;
        } else if (lowestAncestor.isLeaf()){
            // If the lowestAncestor is a leaf, then that means the Trie contains the key itself.
            // So return the predecessor
            return lowestAncestor;
        } else if(lowestAncestor.hasChild0()) {
            return lowestAncestor.child1.succ;
        } else {
            return lowestAncestor.child0;
        }
    }

    private Pair<Node, Node> floorCeilingNode(int key) {
        final var lowestAncestor = searchLowestAncestor(key);

        if(lowestAncestor == null) {
            return null;
        } else if (lowestAncestor.isLeaf()){
            // If the lowestAncestor is a leaf, then that means the Trie contains the key itself.
            // So return the predecessor
            return new Pair<>(lowestAncestor, lowestAncestor);
        } else if(lowestAncestor.hasChild0()) {
            return new Pair<>(lowestAncestor.child1, lowestAncestor.child1.succ);
        } else {
            return new Pair<>(lowestAncestor.child0.pred, lowestAncestor.child0);
        }
    }

    private Node root() {
        return lss[32].get(0);
    }

    public void forEach(BiConsumer<Integer, T> consumer) {
        var current = root().smallestLeaf();
        while(current != null) {
            consumer.accept(current.key, current.content);
            current = current.succ;
        }
    }

    public Collection<T> values() {
        if(root() == null) return List.of();
        List<T> list = new ArrayList<>(lss[0].size());
        var current = root().smallestLeaf();
        while (current != null) {
            list.add(current.content);
            current = current.succ;
        }
        return list;
    }

    public Collection<T> valueRange(Integer from, boolean fromInclusive, Integer to, boolean toInclusive) {
        if(root() == null) return List.of();
        List<T> list = new ArrayList<>(lss[0].size());

        Node current;
        if (fromInclusive) {
            current = ceilingNode(from);
        } else {
            current = ceilingNode(from + 1);
        };

        while (current != null && ((toInclusive && current.key <= to) || (!toInclusive && current.key < to))) {
            list.add(current.content);
            current = current.succ;
        }
        return list;
    }

    @Override
    public Iterator<T> valueRangeIterator(Integer from, boolean fromInclusive, Integer to, boolean toInclusive) {
        return new Iterator<>() {
            Node current = fromInclusive ? ceilingNode(from) : ceilingNode(from + 1);

            @Override
            public boolean hasNext() {
                if(toInclusive) {
                    return current != null && current.key <= to;
                } else {
                    return current != null && current.key < to;
                }
            }

            @Override
            public T next() {
                if(!hasNext()) throw new NoSuchElementException();
                var temp = current.content;
                current = current.succ;
                return temp;
            }
        };
    }

    public int size() {
        return size;
    }

    public Iterator<T> valueIterator() {
        return new ValueIterator();
    }

    private class ValueIterator implements Iterator<T> {

        Node current;
        int expectedSize;

        public ValueIterator() {
            this.current = root() != null ? root().smallestLeaf() : null;
            this.expectedSize = size();
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return this.current != null;
        }

        @Override
        public T next() {
            checkModification();
            if(!hasNext()) throw new NoSuchElementException();

            final var value = this.current.content;
            this.current = this.current.succ;
            return value;
        }

        @Override
        public void remove() {
            checkModification();
            if(this.current != null && this.current.pred != null) {
                final var key = this.current.pred.key;
                XFastTrie.this.remove(key);
                expectedSize--;
            }
        }

        private void checkModification() {
            if(expectedSize != size()) throw new ConcurrentModificationException();
        }
    }

    private class Node {

        /**
         * This node's key
         */
        int key;

        /**
         * The index in {@link XFastTrie#lss} in which this Node is contained
         */
        int level;

        /**
         * This node's content. Only leaves contain content
         */
        T content;


        /**
         * The predecessor of this Node. Only leaves have this
         */
        Node pred;

        /**
         * The successor of this Node. Only leaves have this
         */
        Node succ;

        /**
         * The child which has '0' as its next index
         */
        Node child0;
        boolean isDescendant0;

        /**
         * The child which has '0' as its next index
         */
        Node child1;
        boolean isDescendant1;

        public Node() {
            this(0, 32, null);
        }

        public Node(int key, int level) {
            this(key, level, null);
        }

        public Node(int key, T content) {
            this(key, 0, content);
        }

        private Node(int key, int level, T content) {
            this.key = key;
            this.level = level;
            this.content = content;
            this.child0 = null;
            this.child1 = null;
            this.isDescendant0 = true;
            this.isDescendant1 = true;
        }

        boolean hasChild0() {
            return child0 != null && !isDescendant0;
        }

        boolean hasChild1() {
            return child1 != null && !isDescendant1;
        }

        boolean isLeaf() {
            return child0 == null && child1 == null;
        }

        Node smallestLeaf() {
            Node current = this;
            while (!current.isLeaf()) {
                if(current.hasChild0()) {
                    current = current.child0;
                } else {
                    current = current.child1;
                }
            }
            return current;
        }

        Node greatestLeaf() {
            Node current = this;
            while (!current.isLeaf()) {
                if(current.hasChild1()) {
                    current = current.child1;
                } else {
                    current = current.child0;
                }
            }
            return current;
        }

        void connectChild0(Node child, boolean descendant) {
            child0 = child;
            isDescendant0 = descendant;
        }

        void connectChild1(Node child, boolean descendant) {
            child1 = child;
            isDescendant1 = descendant;
        }

        boolean isDirectChild(Node possibleDescendant) {
            return possibleDescendant.level == this.level - 1;
        }

        boolean isChild1() {
            return IntUtils.bitAt(key, 31 - level) > 0;
        }

        boolean isChild0() {
            return IntUtils.bitAt(key, 31 - level) == 0;
        }

        void disconnectSucc() {
            if (succ == null) return;
            succ.pred = null;
            succ = null;
        }

        void disconnectPred() {
            if (pred == null) return;
            pred.succ = null;
            pred = null;
        }

        void connectSucc(Node other) {
            disconnectSucc();
            if(other == null) {
                succ = null;
                return;
            }

            other.pred = this;
            this.succ = other;
        }

        void connectPred(Node other) {
            disconnectPred();
            if(other == null) {
                pred = null;
                return;
            }

            other.succ = this;
            this.pred = other;
        }

        @Override
        public String toString() {
            if(isLeaf()) {
                return new StringJoiner(", " ,"(", ")").add(Integer.toBinaryString(key)).add(content.toString()).toString();
            } else {
                final String binary = String.format("%32s", Integer.toBinaryString(key)).replace(' ', '0');
                return binary.substring(0, binary.length() - level) + binary.substring(binary.length() - level).replaceAll("[01]", "X");
            }
        }
    }
}
