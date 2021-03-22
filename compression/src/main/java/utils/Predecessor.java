package utils;

import java.util.Collection;
import java.util.Iterator;

public interface Predecessor<K, V> {

    V get(K key);
    V put(K key, V value);
    V remove(K key);

    default V getOrDefault(K key, V def) {
        final var val = get(key);
        return val != null ? val : def;
    }

    Entry<K, V> floorEntry(K key);
    Entry<K, V> ceilingEntry(K key);
    Entry<K, V> higherEntry(K key);
    Entry<K, V> lowerEntry(K key);

    Collection<V> values();
    Collection<V> valueRange(K from, boolean fromInclusive, K to, boolean toInclusive);

    default Iterator<V> valueRangeIterator(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return valueRange(from, fromInclusive, to, toInclusive).iterator();
    };

    record Entry<K, V>(K key, V value) {}
}
