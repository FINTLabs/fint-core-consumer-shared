package no.fintlabs.core.consumer.shared.resource;

import lombok.extern.slf4j.Slf4j;
import no.fint.antlr.FintFilterService;
import no.fint.event.model.HeaderConstants;
import no.fint.model.resource.AbstractCollectionResources;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.core.consumer.shared.resource.exception.FintExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@ControllerAdvice(basePackageClasses = FintExceptionHandler.class)
public abstract class ConsumerRestController<T extends FintLinks & Serializable> {

    private final CacheService<T> cacheService;
    protected final FintLinker<T> fintLinks;
    private final FintFilterService oDataFilterService;
    private final List<CustomIdentificatorHandler<T>> identificatorHandlers = new ArrayList<>();

    protected ConsumerRestController(CacheService<T> cacheService, FintLinker<T> fintLinks, FintFilterService oDataFilterService) {
        this.cacheService = cacheService;
        this.fintLinks = fintLinks;
        this.oDataFilterService = oDataFilterService;
    }

    @GetMapping("/last-updated")
    public Map<String, String> lastUpdatedUrl(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        String lastUpdated = Long.toString(cacheService.getLastUpdated());
        return Map.of("lastUpdated", lastUpdated);
    }

    @GetMapping("/cache/size")
    public Map<String, Integer> cacheSizeUrl(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        return Map.of("size", cacheService.getCacheSize());
    }

    // TODO: 29/07/2022 Trond - Output endret fra FravarResources til AbstractCollectionResources<FravarResource>
    
    @GetMapping
    public AbstractCollectionResources<T> collectionUrl(
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

    @GetMapping("/{idName}/{idValue:.+}")
    public ResponseEntity<T> oneUrl(@PathVariable String idName,
                                    @PathVariable String idValue,
                                    @RequestHeader(name = no.fint.event.model.HeaderConstants.ORG_ID, required = false) String orgId,
                                    @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client) {

        log.debug("idNavn: {}, idVerdi: {}, OrgId: {}, Client: {}", idName, idValue, orgId, client);

        Optional<CustomIdentificatorHandler<T>> identificatorController = identificatorHandlers.stream().filter(i -> i.getName().equalsIgnoreCase(idName)).findFirst();

        if (identificatorController.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        T resource = identificatorController.get().findResourceByIdentifier(cacheService.getCache(), idValue);
        return ResponseEntity.ok(fintLinks.toResource(resource));
    }

    protected void registerIdenficatorHandler(String idName, Function<T, Identifikator> getIdentificatorFunction) {
        CustomIdentificatorHandler<T> handler = new CustomIdentificatorHandler<>(idName, getIdentificatorFunction);
        identificatorHandlers.add(handler);
    }
}
