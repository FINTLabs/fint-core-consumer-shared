package no.fintlabs.core.consumer.shared.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.core.consumer.shared.resource.event.EventResponseCacheService;
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

    private final ConsumerConfig consumerConfig;

    private final EventKafkaProducer eventKafkaProducer;

    private final EventResponseCacheService eventResponseCacheService;

    public WriteableConsumerRestController(
            CacheService<T> cacheService,
            FintLinker<T> fintLinker,
            ConsumerConfig consumerConfig,
            EventKafkaProducer eventKafkaProducer, EventResponseCacheService eventResponseCacheService) {
        super(cacheService, fintLinker);
        this.consumerConfig = consumerConfig;
        this.eventKafkaProducer = eventKafkaProducer;
        this.eventResponseCacheService = eventResponseCacheService;
    }

    @GetMapping("/status/{id}")
    public ResponseEntity getStatus(
            @PathVariable String id,
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(HeaderConstants.CLIENT) String client) {
        log.debug("/status/{} for {} from {}", id, orgId, client);
        ResponseFintEvent responseFintEvent = eventResponseCacheService.get(id);
        if (responseFintEvent == null) {
            return ResponseEntity.accepted().build();
        } else if (responseFintEvent.isFailed()) {
            log.info("EventResponse corrId: {} has failed: {}", id, responseFintEvent.getErrorMessage());
            return ResponseEntity.internalServerError().body(responseFintEvent.getErrorMessage());
        } else if (responseFintEvent.isRejected()) {
            log.info("EventResponse corrId: {} is rejected: {}", id, responseFintEvent.getErrorMessage());
            return ResponseEntity.badRequest().body(responseFintEvent.getRejectReason());
        } else {
            // Todo Burde request/response inneholde selflink?
            // Todo Burde man sjekke om entity er oppdatert.
            return ResponseEntity.created(URI.create("")).build();
        }
    }


    @PostMapping
    public ResponseEntity postBehandling(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody T body,
            @RequestParam(name = "validate", required = false) boolean validate
    ) {
        log.debug("postBehandling, Validate: {}, OrgId: {}, Client: {}", validate, orgId, client);
        log.trace("Body: {}", body);

        // TODO: 13/08/2022 Should mapLinks be called?
//        linker.mapLinks(body);
//        Event event = new Event(orgId, Constants.COMPONENT, SamtykkeActions.UPDATE_BEHANDLING, client);
//        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(body, Map.class));
//        event.setOperation(validate ? Operation.VALIDATE : Operation.CREATE);
//        consumerEventUtil.send(event);
//
//        statusCache.put(event.getCorrId(), event);

        RequestFintEvent event = createRequestEvent(body, RequestFintEvent.OperationType.CREATE);
        eventKafkaProducer.sendEvent(event, EventKafkaProducer.OperationType.CREATE);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    @PutMapping("/systemid/{id:.+}")
    public ResponseEntity putBehandlingBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody T body
    ) {
        log.debug("putBehandlingBySystemId {}, OrgId: {}, Client: {}", id, orgId, client);
        log.trace("Body: {}", body);

//        linker.mapLinks(body);
//        Event event = new Event(orgId, Constants.COMPONENT, SamtykkeActions.UPDATE_BEHANDLING, client);
//        event.setQuery("systemid/" + id);
//        event.addObject(objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).convertValue(body, Map.class));
//        event.setOperation(Operation.UPDATE);
//        fintAuditService.audit(event);
//
//        consumerEventUtil.send(event);
//
//        statusCache.put(event.getCorrId(), event);

        RequestFintEvent event = createRequestEvent(body, RequestFintEvent.OperationType.UPDATE);
        eventKafkaProducer.sendEvent(event, EventKafkaProducer.OperationType.UPDATE);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    private RequestFintEvent createRequestEvent(T body, RequestFintEvent.OperationType operationType) {
        RequestFintEvent event = new RequestFintEvent();
        event.setCorrId(UUID.randomUUID().toString());
        event.setOrgId(consumerConfig.getOrgId());
        event.setDomainName(consumerConfig.getDomainName());
        event.setPackageName(consumerConfig.getPackageName());
        event.setResourceName(consumerConfig.getResourceName());
        event.setOperation(operationType);
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

}
