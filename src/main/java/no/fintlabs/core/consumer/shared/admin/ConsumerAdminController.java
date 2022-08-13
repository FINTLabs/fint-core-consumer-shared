package no.fintlabs.core.consumer.shared.admin;

import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.health.Health;
import no.fintlabs.cache.CacheManager;
import no.fintlabs.core.consumer.shared.ConsumerProps;
import no.fintlabs.core.consumer.shared.resource.CacheService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class ConsumerAdminController {

    private final CacheManager cacheManager;

    private final ConsumerProps consumerProps;

    public ConsumerAdminController(CacheManager cacheManager, ConsumerProps consumerProps) {
        this.cacheManager = cacheManager;
        this.consumerProps = consumerProps;
    }

    @GetMapping("/health")
    public ResponseEntity<Event<Health>> healthCheck(@RequestHeader(HeaderConstants.ORG_ID) String orgId,
                                                     @RequestHeader(HeaderConstants.CLIENT) String client) {

        // TODO: 04/05/2022 Implement when status service is working (Should 303 to status serivce)
        throw new UnsupportedOperationException();

//        log.debug("Health check on {} requested by {} ...", orgId, client);
//        Event<Health> event = new Event<>(orgId, Constants.COMPONENT, DefaultActions.HEALTH, client);
//        event.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.SENT_FROM_CONSUMER_TO_PROVIDER));
//
//        final Optional<Event<Health>> response = consumerEventUtil.healthCheck(event);
//
//        return response.map(health -> {
//            log.debug("Health check response: {}", health.getData());
//            health.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.RECEIVED_IN_CONSUMER_FROM_PROVIDER));
//            return ResponseEntity.ok(health);
//        }).orElseGet(() -> {
//            log.debug("No response to health event.");
//            event.setMessage("No response from adapter");
//            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(event);
//        });
    }

    @GetMapping("/organisations")
    public Collection<String> getOrganisations() {
        return getConsumerServices()
                .stream()
                .map(cacheService -> cacheService.getCacheUrn())
                .collect(Collectors.toList());
    }

    @Deprecated()
    @GetMapping("/organisations/{orgId:.+}")
    public Collection<String> getOrganization(@PathVariable String orgId) {

        return !orgId.equals(consumerProps.getOrgId()) ? Collections.EMPTY_LIST : getOrganisations();
    }

    @GetMapping(value = "/assets")
    public Collection<String> getAssets() {
        return Set.of(consumerProps.getOrgId());
    }

    @GetMapping("/caches")
    public Map<String, Integer> getCaches() {
        return getConsumerServices()
                .stream()
                .collect(Collectors.toMap(
                        CacheService::getCacheUrn,
                        CacheService::getCacheSize)
                );
    }

    @GetMapping("/cache/status")
    public Map<String, Map<String, CacheEntry>> getCacheStatus() {

        if (consumerProps.getOrgId() == null || consumerProps.getOrgId().equals(""))
            throw new IllegalArgumentException("Config for OrgId can not be empty.");

        return getConsumerServices()
                .stream()
                .collect(
                        Collectors.groupingBy(s -> consumerProps.getOrgId(),
                                Collectors.toMap(
                                        s -> s.getResourceName(),
                                        s -> new CacheEntry(new Date(s.getLastUpdated()), s.getCacheSize())
                                )
                        )
                );
    }

    @PostMapping({"/cache/rebuild", "/cache/rebuild/{model}"})
    public void rebuildCache(
            @RequestHeader(required = false, name = HeaderConstants.ORG_ID) String orgid,
            @RequestHeader(required = false, name = HeaderConstants.CLIENT) String client,
            @PathVariable(required = false) String model
    ) {
        log.info("Cache rebuild on {} requested by {}", orgid, client);
        getConsumerServices()
                .stream()
                .filter(cacheService -> StringUtils.isBlank(model) || StringUtils.equalsIgnoreCase(cacheService.getResourceName(), model))
                .forEach(cacheService -> cacheService.resetCache());
    }

    protected abstract Collection<CacheService<?>> getConsumerServices();
}
