package utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class Benchmark {

    private static final boolean ENABLE = true;

    private static final Map<String, Map<String, Long>> EXECUTION_TIMES = new TreeMap<>();
    private static final Map<String, Map<String, Long>> TIMERS = new HashMap<>();

    public static void startTimer(String algorithmName, String counterName) {
        if(!ENABLE) return;
        TIMERS.computeIfAbsent(algorithmName, k -> new HashMap<>());
        final var previous = TIMERS.get(algorithmName).put(counterName, System.nanoTime());
        if(previous != null) throw new TimerAlreadyStartedException(algorithmName, counterName);
    }

    public static void stopTimer(String algorithmName, String counterName) {
        if(!ENABLE) return;
        if(!TIMERS.containsKey(algorithmName) || !TIMERS.get(algorithmName).containsKey(counterName)) {
            throw new NoTimerStartedException(algorithmName, counterName);
        }
        updateTime(algorithmName, counterName, System.nanoTime() - TIMERS.get(algorithmName).remove(counterName));
    }

    public static void updateTime(String algorithmName, String counterName, long timeNs) {
        if(!ENABLE) return;
        if(!EXECUTION_TIMES.containsKey(algorithmName)) {
            EXECUTION_TIMES.put(algorithmName, new LinkedHashMap<>());
        }
        EXECUTION_TIMES.computeIfAbsent(algorithmName, name -> new LinkedHashMap<>());
        EXECUTION_TIMES.get(algorithmName).merge(counterName, timeNs, Long::sum);
    }

    public static Map<String, Long> getAlgorithmValues(String algorithmName) {
        return EXECUTION_TIMES.getOrDefault(algorithmName, new HashMap<>());
    }

    public static Map<String, Map<String, Long>> getAllValues() {
        return EXECUTION_TIMES;
    }


    public static class TimerAlreadyStartedException extends RuntimeException {
        private TimerAlreadyStartedException(String algorithmName, String counterName) {
            super(String.format("Timer \"%s\" already started for algorithm \"%s\"", counterName, algorithmName));
        }
    }

    public static class NoTimerStartedException extends RuntimeException {
        private NoTimerStartedException(String algorithmName, String counterName) {
            super(String.format("No Timer \"%s\" started for algorithm \"%s\"", counterName, algorithmName));
        }
    }
}
