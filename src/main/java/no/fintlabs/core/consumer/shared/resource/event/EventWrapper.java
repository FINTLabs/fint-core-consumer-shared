package no.fintlabs.core.consumer.shared.resource.event;

import lombok.Data;
import no.fintlabs.adapter.models.FintEvent;

import java.util.Date;

@Data
public class EventWrapper<T extends FintEvent> {

    private T fintEvent;
    private Date created;

    public EventWrapper(T fintEvent) {
        this.fintEvent = fintEvent;
        created = new Date();
    }
}
