package no.fintlabs.core.consumer.shared.resource.event;

import lombok.Data;
import no.fintlabs.adapter.models.ResponseFintEvent;

import java.util.Date;

@Data
public class ResponseEventWrapper {
    private ResponseFintEvent responseFintEvent;
    private Date created;

    public ResponseEventWrapper(ResponseFintEvent responseFintEvent) {
        this.responseFintEvent = responseFintEvent;
        created = new Date();
    }
}
