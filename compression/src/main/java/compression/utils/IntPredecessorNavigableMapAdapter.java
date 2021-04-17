package compression.utils;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

public class IntPredecessorNavigableMapAdapter<V> implements IntPredecessor<V> {

    private NavigableMap<Integer, V> map;

    public IntPredecessorNavigableMapAdapter(NavigableMap<Integer, V> map) {
        this.map = map;
    }

    @Override
    public V get(int key) {
        return map.get(key);
    }

    @Override
    public V put(int key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(int key) {
        return map.remove(key);
    }

    @Override
    public IntEntry<V> floorEntry(int key) {
        final Map.Entry<Integer, V> mapEntry = map.floorEntry(key);
        return mapEntry != null ? new IntEntry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public IntEntry<V> ceilingEntry(int key) {
        final Map.Entry<Integer, V> mapEntry = map.ceilingEntry(key);
        return mapEntry != null ? new IntEntry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public IntEntry<V> higherEntry(int key) {
        final Map.Entry<Integer, V> mapEntry = map.higherEntry(key);
        return mapEntry != null ? new IntEntry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public IntEntry<V> lowerEntry(int key) {
        final Map.Entry<Integer, V> mapEntry = map.lowerEntry(key);
        return mapEntry != null ? new IntEntry<>(mapEntry.getKey(), mapEntry.getValue()) : null;
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Collection<V> valueRange(int from, boolean fromInclusive, int to, boolean toInclusive) {
        return map.subMap(from, fromInclusive, to, toInclusive).values();
    }


}
