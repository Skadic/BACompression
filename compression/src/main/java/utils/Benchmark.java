package utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A class offering functions for recording execution time mainly through {@link #startTimer(String, String)} and {@link #stopTimer(String, String)}.
 * Results can be retrieved through {@link #getGroupBenchmarkData(String)} or {@link #getAllValues()}.
 */
public final class Benchmark {

    /**
     * Whether recording benchmark data is enabled
     */
    private static final boolean ENABLE = true;

    /**
     * A map that maps `group name -> timer name -> benchmark data`. This records all benchmark data
     */
    private static final Map<String, Map<String, BenchmarkData>> EXECUTION_TIMES = new TreeMap<>();

    /**
     * A temporary storage for running timers. It maps `group name -> timer name -> timer start time`.
     * The start time is recorded in nanoseconds.
     *
     * @see System#nanoTime()
     */
    private static final Map<String, Map<String, Long>> TIMERS = new HashMap<>();

    /**
     * Starts a timer to record execution time. The timer can be stopped using {@link #stopTimer(String, String)},
     * which stops the timer and adds the recorded time to the corresponding value in {@link #EXECUTION_TIMES}
     * @param group The name of the group which the timer belongs to
     * @param timerName The name of the timer
     *
     * @see #stopTimer(String, String)
     * @see #updateTime(String, String, long)
     */
    public static void startTimer(String group, String timerName) {
        if(!ENABLE) return;
        TIMERS.computeIfAbsent(group, k -> new HashMap<>());
        final var previous = TIMERS.get(group).put(timerName, System.nanoTime());
        if(previous != null) throw new TimerAlreadyStartedException(group, timerName);
    }

    /**
     * Stops a timer started by {@link #startTimer(String, String)}. The recorded time is added to the {@link #EXECUTION_TIMES}
     * map. This calls {@link #updateTime(String, String, long)} internally.
     * @param group The group which this timer belongs to
     * @param timerName The name of this timer
     *
     * @see #startTimer(String, String)
     * @see #updateTime(String, String, long)
     */
    public static void stopTimer(String group, String timerName) {
        if(!ENABLE) return;
        if(!TIMERS.containsKey(group) || !TIMERS.get(group).containsKey(timerName)) {
            throw new NoTimerStartedException(group, timerName);
        }
        updateTime(group, timerName, System.nanoTime() - TIMERS.get(group).remove(timerName));
    }

    /**
     * Adds the given time to a timer. For ease of use, rather use {@link #startTimer(String, String)} and {@link #stopTimer(String, String)}
     * than this.
     * @param group The group which this timer belongs to
     * @param timerName The name of this timer
     * @param timeNs The time to add to the timer in nanoseconds
     */
    public static void updateTime(String group, String timerName, long timeNs) {
        if(!ENABLE) return;
        if(!EXECUTION_TIMES.containsKey(group)) {
            EXECUTION_TIMES.put(group, new LinkedHashMap<>());
        }
        EXECUTION_TIMES.computeIfAbsent(group, name -> new LinkedHashMap<>()) // get the map which contains the benchmark data for the given algorithm
                .computeIfAbsent(timerName, n -> new BenchmarkData()) // Get the benchmark data that tracks this timer name
                .addTime(timeNs); // Add the new time to it
    }

    /**
     * Gets all the benchmark data in a group.
     * @param group The group whose values to get
     * @return A map, that maps from timer name to its corresponding benchmark data
     */
    public static Map<String, BenchmarkData> getGroupBenchmarkData(String group) {
        return EXECUTION_TIMES.getOrDefault(group, new HashMap<>());
    }

    /**
     * Gets all benchmark values.
     * @return A map that marks from a group name, to another map which maps from timer names to their corresponding benchmark data
     *
     * @see #getGroupBenchmarkData(String)
     */
    public static Map<String, Map<String, BenchmarkData>> getAllValues() {
        return EXECUTION_TIMES;
    }


    /**
     * An exception thrown when trying to start a timer while it is already running
     */
    public static class TimerAlreadyStartedException extends RuntimeException {
        private TimerAlreadyStartedException(String group, String counterName) {
            super(String.format("Timer \"%s\" already started for algorithm \"%s\"", counterName, group));
        }
    }

    /**
     * An exception thrown when trying to stop a timer when no timer is actually started
     */
    public static class NoTimerStartedException extends RuntimeException {
        private NoTimerStartedException(String group, String counterName) {
            super(String.format("No Timer \"%s\" started for algorithm \"%s\"", counterName, group));
        }
    }

    /**
     * A class holding data about benchmark results.
     */
    public static class BenchmarkData {
        /**
         * The total execution time
         */
        private long timeNs;

        /**
         * The amount of times the timer has been called
         */
        private long callCount;

        public BenchmarkData() {
            this.timeNs = 0;
            this.callCount = 0;
        }

        /**
         * Adds time and increases {@link #callCount} by one
         * @param timeNs The time to add
         */
        private void addTime(long timeNs) {
            this.timeNs += timeNs;
            this.callCount++;
        }

        /**
         * Gets the total execution time in nanoseconds
         * @return The total execution time in nanoseconds
         */
        public long timeNanos() {
            return timeNs;
        }

        /**
         * Gets the total execution time in milliseconds
         * @return The total execution time in milliseconds
         */
        public long timeMillis() {
            return timeNs / 1000_000;
        }

        /**
         * Gets the amount of times the timer has been called
         * @return The amount of times the timer has been called
         */
        public long callCount() {
            return callCount;
        }

        @Override
        public String toString() {
            return "%dms, %d calls".formatted(timeMillis(), callCount);
        }
    }
}
