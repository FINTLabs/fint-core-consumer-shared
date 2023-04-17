package no.fintlabs.core.consumer.shared.resource.event;

import lombok.Data;
import no.fint.model.resource.FintLinks;

import java.io.Serializable;
import java.util.Date;

@Data
public class EventEntityCacheElement<T extends FintLinks & Serializable> {

    private T entity;
    private Date created;

    public EventEntityCacheElement(T entity) {
        this.entity = entity;
        created = new Date();
    }
}
