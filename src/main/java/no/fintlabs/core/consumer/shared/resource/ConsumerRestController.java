package no.fintlabs.core.consumer.shared.resource;

import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.FintFilterService;
import no.fint.event.model.HeaderConstants;
import no.fint.model.resource.AbstractCollectionResources;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.core.consumer.shared.resource.exception.EntityNotFoundException;
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
    public AbstractCollectionResources<T> getResource(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(defaultValue = "0") long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String $filter)
            {
        log.debug("OrgId: {}, Client: {}", orgId, client);

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

        return fintLinks.toResources(resources, offset, size, cacheService.getCacheSize());
    }

    @GetMapping("/systemid/{id:.+}")
    public T getResourceBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) {

        log.debug("systemId: {}, OrgId: {}, Client: {}", id, orgId, client);
        Optional<T> resource = cacheService.getBySystemId(id);
        return resource.map(fintLinks::toResource).orElseThrow(() -> new EntityNotFoundException(id));
    }

}
