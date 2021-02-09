package repair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RePair {

    private int currentId;
    private int len;
    private SymbolContainer[] sequence;
    private final RePairQueue queue;

    public RePair(String s) {
        len = s.length();
        currentId = 0;
        final var index = new AtomicInteger(0);
        sequence = s.chars()
                .mapToObj(Terminal::new)
                .map(terminal -> new SymbolContainer(terminal, index.getAndIncrement()))
                .toArray(SymbolContainer[]::new);

        queue = new RePairQueue(len);

        populateQueue();
    }

    private void populateQueue() {
        // A map that stores the previous occurrence index of each pair
        var prevOccured = new HashMap<SymbolPair, Integer>();
        // The amount of pairs that consist of 2 of the same characters
        // This is set to 0, when a pair of different characters is read
        // Otherwise, it is incremented by 1
        var sameChar = 0;

        // Add all occuring pairs to the queue
        for (int i = 0; i < sequence.length - 1; i++) {
            SymbolContainer left = sequence[i];
            SymbolContainer right = sequence[i + 1];

            // Link the two containers together, so that later if empty spaces open in the array, the neighboring
            // symbols can be easily found
            left.connectNext(right);

            SymbolPair pair = new SymbolPair(left.getSymbol(), right.getSymbol());

            if (!queue.contains(pair)) {
                // This is the first time we've seen this pair, so this is its first occurrence
                pair.setFirstOccurrence(i);
            }

            if(left.equals(right)) {
                sameChar++;
            } else {
                sameChar = 0;
            }

            switch (sameChar) {
                case 0, 1 -> {
                    // If we have seen this pair, link the last/next occurrence index of the pair
                    if (prevOccured.containsKey(pair)) {
                        int prevOccurence = prevOccured.get(pair);
                        SymbolContainer prev = sequence[prevOccurence];
                        prev.setNextOccurence(i);
                        left.setPrevOccurence(prevOccurence);
                    }
                    queue.add(pair);
                }
                // In this case, we have something like "aaa" and the last char being read was the third 'a'
                // Then the "nextOcc" of the first "aa" is not the 2nd "aa" as they overlap
                case 2 -> {
                    // Get the previous occurrence before the first "aa" in the "aaa"
                    int prevOccurence = sequence[i - 1].getPrevOccurence();
                    left.setPrevOccurence(prevOccurence);
                }
                // In this case the last non-overlapping
                default -> {
                    int prevOccurence = i - 2;
                    SymbolContainer prev = sequence[prevOccurence];
                    prev.setNextOccurence(i);
                    left.setPrevOccurence(prevOccurence);
                    queue.add(pair);
                }
            }

            prevOccured.put(pair, i);

        }

        // Set the last occurrence for each pair
        prevOccured.forEach(SymbolPair::setLastOccurrence);
    }

    public List<SymbolContainer> getSequence() {
        return List.of(sequence);
    }

    public void compress() {
        var done = false;
        while (!done) {
            done = !replaceMostFrequent();
            //compact();
        };
    }

    private void compact() {
        var newIndex = 0;

        var indexMap = new TreeMap<Integer, Integer>();
        var list = new ArrayList<SymbolContainer>();

        // Add the old non-empty elements to the list and record the new indices of the elements
        for (int i = 0; i < sequence.length; i++) {
            SymbolContainer symbolContainer = sequence[i];
            if(symbolContainer.isEmpty()) {
                continue;
            }

            list.add(symbolContainer);
            indexMap.put(i, newIndex);
            newIndex++;
        }

        // Update the previous and next occurrences
        for (SymbolContainer container : list) {
            int prev = container.getPrevOccurence();
            int next = container.getNextOccurence();

            if (prev != -1) {
                container.setPrevOccurence(indexMap.get(prev));
            }

            if(next != -1) {
                container.setNextOccurence(indexMap.get(next));
            }
        }

        var processed = new HashSet<SymbolPair>();

        for (int i = 0; i < list.size() - 1; i++) {
            var left = list.get(i).getSymbol();
            var right = list.get(i + 1).getSymbol();
            var pair = queue.getInstance(left, right);

            if(!processed.contains(pair)) {
                pair.setFirstOccurrence(indexMap.get(pair.getFirstOccurrence()));
                pair.setLastOccurrence(indexMap.get(pair.getLastOccurrence()));
                processed.add(pair);
            }
        }


        System.out.println();
        System.out.println("TheList: " + list);
        System.out.println("NextOcc: " + list.stream().map(SymbolContainer::getNextOccurence).collect(Collectors.toList()));
        System.out.println("PrevOcc: " + list.stream().map(SymbolContainer::getPrevOccurence).collect(Collectors.toList()));

        sequence = list.toArray(new SymbolContainer[0]);
    }

    public boolean replaceMostFrequent() {
        //System.out.println(queue);
        final var pair = queue.poll();

        // There are no more pairs to replace
        if (pair == null) {
            return false;
        }

        var currentIndex = pair.getFirstOccurrence();
        pair.mark(currentId++);

        // Stores the last occurrence index of the pair, where the symbol following the pair is the given symbol
        var previousOccurrenceMap = new HashMap<Symbol, Integer>();

        while (currentIndex != -1) {
            // Get the left and right symbol in the pair
            var left = sequence[currentIndex];
            var right = left.next();

            // If they have the respective neighbor, decrease their frequency in the queue
            if (left.hasPrev()) {
                var before = new SymbolPair(left.prev().getSymbol(), left.getSymbol());
                updateOccurrences(before, left.prev().getIndex());
                queue.updateFrequency(before, -1);
            }
            if (right.hasNext()) {
                var after = new SymbolPair(right.getSymbol(), right.next().getSymbol());
                updateOccurrences(after, right.getIndex());
                queue.updateFrequency(after, -1);
            }

            left.setSymbol(pair);
            right.setSymbol(null);

            left.connectNext(right.next());

            if(right.hasNext()) {
                Symbol followingSymbol = right.next().getSymbol();
                left.setPrevOccurence(previousOccurrenceMap.getOrDefault(followingSymbol, -1));
                if(previousOccurrenceMap.containsKey(followingSymbol)) {
                    sequence[previousOccurrenceMap.get(followingSymbol)].setNextOccurence(currentIndex);
                }
            } else {
                left.setPrevOccurence(-1);
            }

            // This space is now empty and thus has no sensible values
            right.setNextOccurence(-1);
            right.setPrevOccurence(-1);

            if (left.hasPrev()) {
                var before = new SymbolPair(left.prev().getSymbol(), left.getSymbol());
                queue.add(before);
                updateOccurrences(before, left.prev().getIndex());
            }

            if (left.hasNext()) {
                var after = new SymbolPair(left.getSymbol(), left.next().getSymbol());
                queue.add(after);
                updateOccurrences(after, right.getIndex());
            }

            // If the two symbols before the new one are the same as the new one, then that means they are overlapping
            // occurrences of the new symbols. Both should not be considered.
            if(left.hasPrev() && left.prev().hasPrev() && left.equals(left.prev()) && left.equals(left.prev().prev())) {
                var before = new SymbolPair(left.prev().getSymbol(), left.getSymbol());
                queue.updateFrequency(before, -1);
            }

            if(right.hasNext()) previousOccurrenceMap.put(right.next().getSymbol(), currentIndex);
            currentIndex = left.getNextOccurence();
            left.setNextOccurence(-1);
        }

        return true;
    }

    private void updateOccurrences(SymbolPair pair, int index) {
        if(queue.contains(pair)) {
            SymbolPair instance = queue.getInstance(pair);
            var oldFirstOccurrence = instance.getFirstOccurrence();
            var oldLastOccurrence = instance.getLastOccurrence();
            if (index < oldFirstOccurrence || oldFirstOccurrence == -1) {
                instance.setFirstOccurrence(index);
            }

            if (index > oldLastOccurrence || oldLastOccurrence == -1) {
                instance.setLastOccurrence(index);
            }
        }
    }

    public String getAllRules(boolean shortRep) {
        List<Symbol> symbols = Arrays.stream(sequence)
                .map(SymbolContainer::getSymbol)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<SymbolPair> pairs = new TreeSet<>(Comparator.comparingInt(SymbolPair::getId));
        Map<Integer, Integer> usages = new HashMap<>();

        StringBuilder rules = new StringBuilder("Usage      Rule\n");
        StringJoiner startRule = new StringJoiner(" ", "S  -> ", "\n");


        for (Symbol symbol : symbols) {
            startRule.add(symbol.toStringInternal(true));
            if (symbol instanceof SymbolPair pair) {
                usages.merge(pair.getId(), 1, Integer::sum);
                var symbolRules = symbol.getAllRules();
                pairs.addAll(symbolRules);
            }
        }


        for (SymbolPair pair : pairs) {
            if (pair.getLeft() instanceof SymbolPair left) {
                usages.merge(left.getId(), 1, Integer::sum);
            }
            if (pair.getRight() instanceof SymbolPair right) {
                usages.merge(right.getId(), 1, Integer::sum);
            }
        }


        rules.append(" 0   ").append(startRule.toString());
        for(var pair : pairs) {
            StringJoiner rule = new StringJoiner(" ", "R" + pair.getId() + " -> ", "\n");
            rule.add(pair.getLeft().toStringInternal(true));
            rule.add(pair.getRight().toStringInternal(true));
            rules.append(String.format(" %-3d %-3d %s", pair.getId(), usages.get(pair.getId()), rule.toString()));
        }

        int totalCount = usages.values().stream().reduce(Integer::sum).orElse(0);

        if(shortRep) rules = new StringBuilder();

        rules.append("Rule set size: ").append(totalCount);

        return rules.toString();
    }


    public int nextOccurence(int i) {
        return sequence[i].getNextOccurence();
    }

    public int lastOccurrence(int i) {
        return sequence[i].getPrevOccurence();
    }

    public RePairQueue getQueue() {
        return queue;
    }
}
