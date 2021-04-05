package compression.utils;

import java.util.Collection;
import java.util.Iterator;

public interface Predecessor<K, V> {

    /**
     * Get the associated value for a key
     * @param key The key whose mapping to get
     * @return The mapped value if it exists, null if there is no such mapping
     */
    V get(K key);

    /**
     * Puts a mapping into the data structure and returns the value of the previous mapping
     *
     * @param key The key to associate with the value
     * @param value The value to associate with the key
     * @return The previous mapped value if there was any, null otherwise
     */
    V put(K key, V value);

    /**
     * Remove a mapping from the data structure and return the removed value
     * @param key The key whose mapping to remove
     * @return The removed value if it exists, null if there was no such mapping
     */
    V remove(K key);

    default V getOrDefault(K key, V def) {
        final var val = get(key);
        return val != null ? val : def;
    }

    /**
     * Returns the greatest value in the data structure whose key is lesser or equal to the given key
     * @param key The key
     * @return The greatest value in the data structure whose key is lesser or equal to the given key
     */
    Entry<K, V> floorEntry(K key);

    /**
     * Returns the greatest value in the data structure whose key is greater or equal to the given key
     * @param key The key
     * @return The greatest value in the data structure whose key is greater or equal to the given key
     */
    Entry<K, V> ceilingEntry(K key);

    /**
     * Returns the greatest value in the data structure whose key is lesser than the given key
     * @param key The key
     * @return The greatest value in the data structure whose key is lesser than the given key
     */
    Entry<K, V> lowerEntry(K key);

    /**
     * Returns the smallest value in the data structure whose key is greater than the given key
     * @param key The key
     * @return The smallest value in the data structure whose key is greater than the given key
     */
    Entry<K, V> higherEntry(K key);

    /**
     * Gets the values contained in this data structure as a {@link Collection} sorted by their keys
     * @return The sorted values as a {@link Collection}
     */
    Collection<V> values();

    /**
     * Gets the values with keys in the specified bounds, contained in this data structure as a {@link Collection} sorted by their keys
     * @param from The lower bound of the keys
     * @param fromInclusive Whether the lower bound is inclusive
     * @param to The upper bound of the keys
     * @param toInclusive Whether the upper bound is inclusive
     * @return The sorted values in the given bounds as a {@link Collection}
     */
    Collection<V> valueRange(K from, boolean fromInclusive, K to, boolean toInclusive);


    /**
     * Gets an iterator over the values with keys in the specified bounds, contained in this data structure sorted by their keys
     * @param from The lower bound of the keys
     * @param fromInclusive Whether the lower bound is inclusive
     * @param to The upper bound of the keys
     * @param toInclusive Whether the upper bound is inclusive
     * @return The sorted values in the given bounds as an {@link Iterator}
     */
    default Iterator<V> valueRangeIterator(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return valueRange(from, fromInclusive, to, toInclusive).iterator();
    }

    record Entry<K, V>(K key, V value) {}
}
