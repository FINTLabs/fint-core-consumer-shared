package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.Cache;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class KafkaEventLogger {

    private final String resourceType;
    private final ScheduledThreadPoolExecutor executor;
    private Runnable task;
    private final AtomicInteger eventCount;
    private long startTimer;
    private int previousCount;

    public KafkaEventLogger(String resourceType, Cache<?> cache) {
        this.resourceType = resourceType;
        eventCount = new AtomicInteger(0);
        executor = new ScheduledThreadPoolExecutor(1);
        task = () -> {
            if (eventCount.get() == previousCount) {
                long endTimer = System.currentTimeMillis();
                String timeTaken = getTimeFormat(endTimer - startTimer);

                log.info("{} received: {} | Time taken: {} | Cache size: {}" , resourceType, eventCount, timeTaken, cache.size());
                previousCount = 0;
                eventCount.set(0);
            } else {
                previousCount = eventCount.get();
                executor.schedule(task, 3, TimeUnit.SECONDS);
            }
        };
    }

    public synchronized void logDataRecieved() {
        if (eventCount.getAndIncrement() == 0) {
            log.info("Started recieving " + resourceType + "...");
            startTimer = System.currentTimeMillis();
            executor.schedule(task, 3, TimeUnit.SECONDS);
        }
    }

    private String getTimeFormat(long milliseconds) {
        long hours = milliseconds / 1000 / 60 / 60;
        long minutes = milliseconds / 1000 / 60 % 60;
        long seconds = milliseconds / 1000 % 60;

        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

}
