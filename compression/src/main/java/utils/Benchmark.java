package utils;

import java.util.HashMap;
import java.util.Map;

public final class Benchmark {

    private static final Map<String, Map<String, Long>> EXECUTION_TIMES = new HashMap<>();


    public static void updateTime(String algorithmName, String counterName, long timeMs) {
        if(!EXECUTION_TIMES.containsKey(algorithmName)) {
            EXECUTION_TIMES.put(algorithmName, new HashMap<>());
        }
        EXECUTION_TIMES.get(algorithmName).merge(counterName, timeMs, Long::sum);
    }

    public static Map<String, Long> getAlgorithmValues(String algorithmName) {
        return EXECUTION_TIMES.getOrDefault(algorithmName, new HashMap<>());
    }

    public static Map<String, Map<String, Long>> getAllValues() {
        return EXECUTION_TIMES;
    }

}
