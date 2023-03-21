package no.fintlabs.core.consumer.shared.resource.event;

import lombok.Data;

import java.util.Date;

@Data
public class EventEntityCacheElement {

    private Object entity;
    private Date created;

    public EventEntityCacheElement(Object entity) {
        this.entity = entity;
        created = new Date();
    }
}
