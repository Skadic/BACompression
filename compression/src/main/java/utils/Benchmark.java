package utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class Benchmark {

    private static final Map<String, Map<String, Long>> EXECUTION_TIMES = new TreeMap<>();


    public static void updateTime(String algorithmName, String counterName, long timeNs) {
        if(!EXECUTION_TIMES.containsKey(algorithmName)) {
            EXECUTION_TIMES.put(algorithmName, new LinkedHashMap<>());
        }
        EXECUTION_TIMES.get(algorithmName).merge(counterName, timeNs, Long::sum);
    }

    public static Map<String, Long> getAlgorithmValues(String algorithmName) {
        return EXECUTION_TIMES.getOrDefault(algorithmName, new HashMap<>());
    }

    public static Map<String, Map<String, Long>> getAllValues() {
        return EXECUTION_TIMES;
    }

}
