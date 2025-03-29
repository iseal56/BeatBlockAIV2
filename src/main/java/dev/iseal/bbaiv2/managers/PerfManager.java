package dev.iseal.bbaiv2.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerfManager {

    private static final PerfManager instance = new PerfManager();
    public static PerfManager getInstance() {
        return instance;
    }

    private final ThreadLocal<Long> startTime = ThreadLocal.withInitial(() -> 0L);
    private final ConcurrentHashMap<String, Long> durations = new ConcurrentHashMap<>();

    public static void push() {
        getInstance().startTime.set(System.nanoTime());
    }

    public static void pop(String utilizer) {
        PerfManager instance = getInstance();
        long duration = System.nanoTime() - instance.startTime.get();
        instance.durations.merge(utilizer, duration, Long::sum);
    }

    public static void popStack() {
        for (Map.Entry<String, Long> entry : getInstance().durations.entrySet()) {
            System.out.println(entry.getKey() + " took " + entry.getValue() / 1000000L + " milliseconds.");
        }
        getInstance().durations.clear();
    }
}