package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class KafkaEventLogger {

    private String resourceType;
    private ScheduledThreadPoolExecutor executor;
    private Runnable task;
    private AtomicInteger eventCount;
    private int previousCount;

    public KafkaEventLogger(String resourceType) {
        this.resourceType = resourceType;
        eventCount = new AtomicInteger(0);
        executor = new ScheduledThreadPoolExecutor(1);
        task = () -> {
            if (eventCount.get() == previousCount) {
                log.info(resourceType + " recieved: " + eventCount);
                previousCount = 0;
                eventCount.set(0);
            } else {
                previousCount = eventCount.get();
                executor.schedule(task, 3, TimeUnit.SECONDS);
            }
        };
    }

    public synchronized void onDataRecieved() {
        if (eventCount.getAndIncrement() == 0) {
            log.info("Started recieving " + resourceType + "...");
            executor.schedule(task, 3, TimeUnit.SECONDS);
        }
    }

}