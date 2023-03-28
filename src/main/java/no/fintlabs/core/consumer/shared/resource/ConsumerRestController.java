package no.fintlabs.core.consumer.shared.resource;

import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.FintFilterService;
import no.fint.event.model.HeaderConstants;
import no.fint.model.resource.AbstractCollectionResources;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.core.consumer.shared.EntityNotFoundException;
import no.fintlabs.core.consumer.shared.resource.exception.FintExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@ControllerAdvice(basePackageClasses = FintExceptionHandler.class)
public abstract class ConsumerRestController<T extends FintLinks & Serializable> {

    private final CacheService<T> cacheService;
    protected final FintLinker<T> fintLinks;
    private final FintFilterService oDataFilterService;

    protected ConsumerRestController(CacheService<T> cacheService, FintLinker<T> fintLinks, FintFilterService oDataFilterService) {
        this.cacheService = cacheService;
        this.fintLinks = fintLinks;
        this.oDataFilterService = oDataFilterService;
    }

    @GetMapping("/last-updated")
    public Map<String, String> getLastUpdated(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {

        String lastUpdated = Long.toString(cacheService.getLastUpdated());
        return Map.of("lastUpdated", lastUpdated);
    }

    @GetMapping("/cache/size")
    public Map<String, Integer> getCacheSize(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {

        return Map.of("size", cacheService.getCacheSize());
    }

    // TODO: 29/07/2022 Trond - Output endret fra FravarResources til AbstractCollectionResources<FravarResource> 
    
    @GetMapping
    public AbstractCollectionResources<T> getFravar(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String $filter)
            {
//        if (cacheService == null) {
//            throw new CacheDisabledException("Fravar cache is disabled.");
//        }
//        if (props.isOverrideOrgId() || orgId == null) {
//            orgId = props.getDefaultOrgId();
//        }
//        if (client == null) {
//            client = props.getDefaultClient();
//        //
        log.debug("OrgId: {}, Client: {}", orgId, client);

//        Event event = new Event(orgId, Constants.COMPONENT, VurderingActions.GET_ALL_FRAVAR, client);
//        event.setOperation(Operation.READ);
//        if (StringUtils.isNotBlank(request.getQueryString())) {
//            event.setQuery("?" + request.getQueryString());
//        }
//        fintAuditService.audit(event);
//        fintAuditService.audit(event, Status.CACHE);


        Stream<T> resources;
        if (size > 0 && offset >= 0 && sinceTimeStamp > 0) {
            resources = cacheService.streamSliceSince(sinceTimeStamp, offset, size);
        } else if (size > 0 && offset >= 0) {
            resources = cacheService.streamSlice(offset, size);
        } else if (sinceTimeStamp > 0) {
            resources = cacheService.streamSince(sinceTimeStamp);
        } else {
            resources = cacheService.streamAll();
        }

        if (StringUtils.isNotBlank($filter)) {
            if (oDataFilterService.validate($filter)) {
                resources = oDataFilterService.from(resources, $filter);
            } else {
                throw new IllegalArgumentException("Odata filter is not valid");
            }
        }

        //fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        return fintLinks.toResources(resources, offset, size, cacheService.getCacheSize());
    }

    @GetMapping("/systemid/{id:.+}")
    public T getFravarBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) {
//        if (props.isOverrideOrgId() || orgId == null) {
//            orgId = props.getDefaultOrgId();
//        }
//        if (client == null) {
//            client = props.getDefaultClient();
//        }
        log.debug("systemId: {}, OrgId: {}, Client: {}", id, orgId, client);

//        Event event = new Event(orgId, Constants.COMPONENT, VurderingActions.GET_FRAVAR, client);
//        event.setOperation(Operation.READ);
//        event.setQuery("systemId/" + id);
//
//        if (cacheService != null) {
//            fintAuditService.audit(event);
//            fintAuditService.audit(event, Status.CACHE);
//
        Optional<T> fravar = cacheService.getBySystemId(id);
//
//            fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);
//
        return fravar.map(fintLinks::toResource).orElseThrow(() -> new EntityNotFoundException(id));
//
//        } else {
//            BlockingQueue<Event> queue = synchronousEvents.register(event);
//            consumerEventUtil.send(event);
//
//            Event response = EventResponses.handle(queue.poll(5, TimeUnit.MINUTES));
//
//            if (response.getData() == null ||
//                    response.getData().isEmpty()) throw new EntityNotFoundException(id);
//
//            FravarResource fravar = objectMapper.convertValue(response.getData().get(0), FravarResource.class);
//
//            fintAuditService.audit(response, Status.SENT_TO_CLIENT);
//
//            return linker.toResource(fravar);
//        }
//        return null;
    }

}
