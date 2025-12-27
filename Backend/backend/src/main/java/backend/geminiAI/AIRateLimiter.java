package backend.geminiAI;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AIRateLimiter {

    private final int MAX_REQUESTS_PER_MINUTE = 100;

    // user -> (timestamp of each AI request)
    private final Map<String, CircularCounter> userRequests = new ConcurrentHashMap<>();

    public boolean allow(String username) {
        CircularCounter counter = userRequests.computeIfAbsent(username, u -> new CircularCounter());
        return counter.incrementAndCheck(MAX_REQUESTS_PER_MINUTE);
    }

    private static class CircularCounter {
        private final long[] timestamps = new long[60];
        private int index = 0;

        synchronized boolean incrementAndCheck(int limit) {
            long now = Instant.now().getEpochSecond();

            // Replace oldest timestamp with now
            timestamps[index] = now;
            index = (index + 1) % timestamps.length;

            // Count how many timestamps are within 60 seconds
            int count = 0;
            for (long t : timestamps) {
                if (now - t < 60) {
                    count++;
                }
            }
            return count <= limit;
        }
    }
}
