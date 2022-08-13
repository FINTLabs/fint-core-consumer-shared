package no.fintlabs.core.consumer.shared.resource;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.kafka.EventKafkaProducer;
import no.fintlabs.core.consumer.shared.resource.kafka.KafkaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.util.UUID;

@Slf4j
public class WriteableConsumerRestController<T extends FintLinks & Serializable> extends ConsumerRestController<T> {

    private final ConsumerConfig consumerConfig;

    private final KafkaService<T> kafkaService;

    public WriteableConsumerRestController(
            CacheService<T> cacheService,
            FintLinker<T> fintLinker,
            ConsumerConfig consumerConfig, KafkaService<T> kafkaService) {
        super(cacheService, fintLinker);
        this.consumerConfig = consumerConfig;
        this.kafkaService = kafkaService;
    }

//    @GetMapping("/status/{id}")
//    public ResponseEntity getStatus(
//            @PathVariable String id,
//            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
//            @RequestHeader(HeaderConstants.CLIENT) String client) {
//        log.debug("/status/{} for {} from {}", id, orgId, client);
//        return statusCache.handleStatusRequest(id, orgId, linker, BehandlingResource.class);
//}

    @PostMapping
    public ResponseEntity postBehandling(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
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

        RequestFintEvent<T> event = createRequestEvent(body, RequestFintEvent.OperationType.CREATE);
        kafkaService.getEventKafkaProducer().sendEvent(event, EventKafkaProducer.OperationType.CREATE);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    @PutMapping("/systemid/{id:.+}")
    public ResponseEntity putBehandlingBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
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

        RequestFintEvent<T> event = createRequestEvent(body, RequestFintEvent.OperationType.UPDATE);
        kafkaService.getEventKafkaProducer().sendEvent(event, EventKafkaProducer.OperationType.UPDATE);

        URI location = UriComponentsBuilder.fromUriString(fintLinks.self()).path("status/{id}").buildAndExpand(event.getCorrId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).build();
    }

    private RequestFintEvent<T> createRequestEvent(T body, RequestFintEvent.OperationType operationType) {
        RequestFintEvent<T> event = new RequestFintEvent<>();
        event.setCorrId(UUID.randomUUID().toString());
        event.setOrgId(consumerConfig.getOrgId());
        event.setDomainName(consumerConfig.getDomainName());
        event.setPackageName(consumerConfig.getPackageName());
        event.setResourceName(consumerConfig.getResourceName());
        event.setOperation(operationType);
        event.setCreated(System.currentTimeMillis());
        event.setValue(body);
        return event;
    }

}
