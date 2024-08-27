package no.fintlabs.core.consumer.shared.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.FintFilterService;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.core.consumer.shared.resource.event.EventRequestKafkaConsumer;
import no.fintlabs.core.consumer.shared.resource.event.EventResponseKafkaConsumer;
import no.fintlabs.core.consumer.shared.resource.kafka.EventKafkaProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.util.UUID;

@Slf4j
public abstract class WriteableConsumerRestController<T extends FintLinks & Serializable> extends ConsumerRestController<T> {

    private final ConsumerConfig<T> consumerConfig;
    private final EventKafkaProducer eventKafkaProducer;
    private final EventResponseKafkaConsumer<T> eventResponseKafkaConsumer;
    private final EventRequestKafkaConsumer<T> eventRequestKafkaConsumer;

    public WriteableConsumerRestController(
            CacheService<T> cacheService,
            FintLinker<T> fintLinker,
            ConsumerConfig<T> consumerConfig,
            EventKafkaProducer eventKafkaProducer,
            EventResponseKafkaConsumer<T> eventResponseKafkaConsumer,
            FintFilterService oDataFilterService,
            EventRequestKafkaConsumer<T> eventRequestKafkaConsumer) {
        super(cacheService, fintLinker, oDataFilterService);
        this.consumerConfig = consumerConfig;
        this.eventKafkaProducer = eventKafkaProducer;
        this.eventResponseKafkaConsumer = eventResponseKafkaConsumer;
        this.eventRequestKafkaConsumer = eventRequestKafkaConsumer;
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> getStatus(
            @PathVariable String id,
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(HeaderConstants.CLIENT) String client) {

        log.debug("/status/{} for {} from {}", id, orgId, client);
        ResponseFintEvent<T> responseFintEvent = eventResponseKafkaConsumer.getCache().get(id);
        RequestFintEvent requestFintEvent = eventRequestKafkaConsumer.getCache().get(id);

        EventStatus eventStatus = determineEventStatus(responseFintEvent, requestFintEvent);

        switch (eventStatus) {
            case NOT_FOUND:
                log.warn("EventResponse corrId: {} has no matching request!", id);
                return ResponseEntity.accepted().build();

            case NO_RESPONSE_YET:
                log.info("EventResponse corrId: {} has no response yet.", id);
                return ResponseEntity.accepted().build();

            case FAILED:
                log.info("EventResponse corrId: {} has failed: {}", id, responseFintEvent.getErrorMessage());
                return ResponseEntity.internalServerError().body(responseFintEvent.getErrorMessage());

            case REJECTED:
                log.info("EventResponse corrId: {} is rejected: {}", id, responseFintEvent.getErrorMessage());
                return ResponseEntity.badRequest().body(responseFintEvent.getRejectReason());

            case OK:
                T entity = eventResponseKafkaConsumer.getEntityCache().get(id);
                if (entity == null) {
                    log.warn("Get status, have response but no updated entity for " + id);
                    return ResponseEntity.accepted().build();
                } else {
                    log.info("EventResponse corrId: {} is ok.", id);
                    URI location = UriComponentsBuilder.fromUriString(fintLinks.getSelfHref(entity)).build().toUri();
                    return ResponseEntity.created(location).body(entity);
                }

            default:
                throw new IllegalStateException("Unexpected value: " + eventStatus);

        }
    }

    @PostMapping
    public ResponseEntity<T> postResource(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody T body,
            @RequestParam(required = false) boolean validate
    ) {
        log.debug("postBehandling, Validate: {}, OrgId: {}, Client: {}", validate, orgId, client);
        log.trace("Body: {}", body);

        // TODO: 13/08/2022 Should mapLinks be called?

        RequestFintEvent event = createRequestEvent(body, OperationType.CREATE);
        eventKafkaProducer.sendEvent(event);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    @PutMapping("/systemid/{id:.+}")
    public ResponseEntity<T> putResourceBySystemid(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody T body
    ) {
        log.debug("putBehandlingBySystemId {}, OrgId: {}, Client: {}", id, orgId, client);
        log.trace("Body: {}", body);

        RequestFintEvent event = createRequestEvent(body, OperationType.UPDATE);
        eventKafkaProducer.sendEvent(event);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    private RequestFintEvent createRequestEvent(T body, OperationType operationType) {
        RequestFintEvent event = new RequestFintEvent();
        event.setCorrId(UUID.randomUUID().toString());
        event.setOrgId(consumerConfig.getOrgId());
        event.setDomainName(consumerConfig.getDomainName());
        event.setPackageName(consumerConfig.getPackageName());
        event.setResourceName(consumerConfig.getResourceName());
        event.setOperationType(operationType);
        event.setCreated(System.currentTimeMillis());
        event.setValue(convertToJson(body));
        return event;
    }

    private String convertToJson(T body) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writer().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private enum EventStatus {
        NOT_FOUND, NO_RESPONSE_YET, FAILED, REJECTED, OK
    }

    private EventStatus determineEventStatus(ResponseFintEvent<T> responseFintEvent, RequestFintEvent requestFintEvent) {
        if (responseFintEvent == null && requestFintEvent == null) {
            return EventStatus.NOT_FOUND;
        } else if (responseFintEvent == null) {
            return EventStatus.NO_RESPONSE_YET;
        } else if (responseFintEvent.isFailed()) {
            return EventStatus.FAILED;
        } else if (responseFintEvent.isRejected()) {
            return EventStatus.REJECTED;
        } else {
            return EventStatus.OK;
        }
    }

}

