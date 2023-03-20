package no.fintlabs.core.consumer.shared.resource.event;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.adapter.models.ResponseFintEvent;

import java.util.Date;

@Getter
public class ResponseEventWrapper {

    @Setter private ResponseFintEvent responseFintEvent;
    @Setter private Object entity;
    private Date created;

    public ResponseEventWrapper() {
        created = new Date();
    }
}
