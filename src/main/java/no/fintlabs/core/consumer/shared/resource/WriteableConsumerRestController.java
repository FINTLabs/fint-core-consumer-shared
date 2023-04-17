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

    private final ConsumerConfig<?> consumerConfig;
    private final EventKafkaProducer eventKafkaProducer;
    private final EventResponseKafkaConsumer eventResponseKafkaConsumer;
    private final EventRequestKafkaConsumer eventRequestKafkaConsumer;

    public WriteableConsumerRestController(
            CacheService<T> cacheService,
            FintLinker<T> fintLinker,
            ConsumerConfig<?> consumerConfig,
            EventKafkaProducer eventKafkaProducer,
            EventResponseKafkaConsumer eventResponseKafkaConsumer,
            FintFilterService oDataFilterService,
            EventRequestKafkaConsumer eventRequestKafkaConsumer) {
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
        ResponseFintEvent<?> responseFintEvent = eventResponseKafkaConsumer.getCache().get(id);
        RequestFintEvent requestFintEvent = eventRequestKafkaConsumer.getCache().get(id);

        if (responseFintEvent == null && requestFintEvent == null) {
            log.warn("EventResponse corrId: {} has no matching request!");
            return ResponseEntity.notFound().build();
        } else if (responseFintEvent == null) {
            log.info("EventResponse corrId: {} has no response yet.");
            return ResponseEntity.accepted().build();
        } else if (responseFintEvent.isFailed()) {
            log.info("EventResponse corrId: {} has failed: {}", id, responseFintEvent.getErrorMessage());
            return ResponseEntity.internalServerError().body(responseFintEvent.getErrorMessage());
        } else if (responseFintEvent.isRejected()) {
            log.info("EventResponse corrId: {} is rejected: {}", id, responseFintEvent.getErrorMessage());
            return ResponseEntity.badRequest().body(responseFintEvent.getRejectReason());
        } else {
            Object entity = eventResponseKafkaConsumer.getEntityCache().get(id);
            if (entity == null) {
                log.warn("Get status, have response but no updated entity for " + id);
                return ResponseEntity.accepted().build();
            } else {
                log.info("EventResponse corrId: {} is ok.");
                return ResponseEntity.created(URI.create("")).body(entity);
            }
        }
    }

    @PostMapping
    public ResponseEntity<T> postResource(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestBody T body,
            @RequestParam(name = "validate", required = false) boolean validate
    ) {
        log.debug("postBehandling, Validate: {}, OrgId: {}, Client: {}", validate, orgId, client);
        log.trace("Body: {}", body);

        // TODO: 13/08/2022 Should mapLinks be called?

        RequestFintEvent event = createRequestEvent(body, OperationType.CREATE);
        eventKafkaProducer.sendEvent(event, OperationType.CREATE);

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
        eventKafkaProducer.sendEvent(event, OperationType.UPDATE);

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

}
