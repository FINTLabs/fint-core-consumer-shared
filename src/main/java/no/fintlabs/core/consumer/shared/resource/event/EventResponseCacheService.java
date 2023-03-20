package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.Link;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EventResponseCacheService {

    private static final int MAX_HOURS_OLD = 4;
    private static final int MILI_TO_HOURS = 60 * 60 * 1000;

    private final Map<String, ResponseEventWrapper> responses;

    public EventResponseCacheService() {
        this.responses = new HashMap<>();
    }

    public void add(ResponseFintEvent responseFintEvent) {
        log.info("Adding new response with corrId: {}", responseFintEvent.getCorrId());
        ResponseEventWrapper wrapper = responses.getOrDefault(responseFintEvent.getCorrId(), new ResponseEventWrapper());
        wrapper.setResponseFintEvent(responseFintEvent);
        responses.put(responseFintEvent.getCorrId(), wrapper);
    }

    public void add(String corrId, Object entity) {
        log.info("Adding selflink for corrId: {}", corrId);
        ResponseEventWrapper wrapper = responses.getOrDefault(corrId, new ResponseEventWrapper());
        wrapper.setEntity(entity);
        responses.put(corrId, wrapper);
    }

    public Optional<ResponseFintEvent> getResponse(String corrId) {
        if (responses.containsKey(corrId)) {
            return Optional.ofNullable(responses.get(corrId).getResponseFintEvent());
        }
        return Optional.empty();
    }

    public Object getEntity(String corrId) {
        return responses.containsKey(corrId) ? responses.get(corrId).getEntity() : null;
    }

    @Scheduled(initialDelay = 600000, fixedDelay = 1800000)
    private void removeOld() {
        long currentTime = System.currentTimeMillis();
        responses.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > MAX_HOURS_OLD * MILI_TO_HOURS);
    }

}
