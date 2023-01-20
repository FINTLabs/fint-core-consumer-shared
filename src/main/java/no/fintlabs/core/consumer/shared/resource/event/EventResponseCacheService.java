package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class EventResponseCacheService {

    private static final int MAX_HOURS_OLD = 4;
    private static final int MILI_TO_HOURS = 60 * 60 * 1000;

    private final Map<String, ResponseEventWrapper> responses;

    public EventResponseCacheService() {
        this.responses = new HashMap<>();
    }

    public void add(ResponseFintEvent responseFintEvent) {
        log.info("Adding new response with corrId: {}", responseFintEvent.getCorrId());
        responses.put(responseFintEvent.getCorrId(), new ResponseEventWrapper(responseFintEvent));
    }

    public ResponseFintEvent get(String corrId) {
        if (responses.containsKey(corrId)) {
            return responses.get(corrId).getResponseFintEvent();
        }
        return null;
    }

    @Scheduled(initialDelay = 600000, fixedDelay = 1800000)
    private void removeOld() {
        long currentTime = System.currentTimeMillis();
        responses.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > MAX_HOURS_OLD * MILI_TO_HOURS);
    }

}
