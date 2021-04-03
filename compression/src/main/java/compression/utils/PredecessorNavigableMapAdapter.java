package compression.utils;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

public class PredecessorNavigableMapAdapter<K, V> implements Predecessor<K, V> {

    private NavigableMap<K, V> map;

    public PredecessorNavigableMapAdapter(NavigableMap<K, V> map) {
        this.map = map;
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(K key) {
        return map.remove(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        final Map.Entry<K, V> mapEntry = map.floorEntry(key);
        return mapEntry != null ? new Entry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        final Map.Entry<K, V> mapEntry = map.ceilingEntry(key);
        return mapEntry != null ? new Entry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        final Map.Entry<K, V> mapEntry = map.higherEntry(key);
        return mapEntry != null ? new Entry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        final Map.Entry<K, V> mapEntry = map.lowerEntry(key);
        return mapEntry != null ? new Entry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Collection<V> valueRange(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return map.subMap(from, fromInclusive, to, toInclusive).values();
    }


}
