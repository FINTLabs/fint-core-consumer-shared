package no.fintlabs.core.consumer.shared.resource;

import no.fint.model.resource.FintLinks;
import no.fintlabs.cache.Cache;
import no.fintlabs.cache.CacheManager;
import no.fintlabs.core.consumer.shared.resource.kafka.KafkaService;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class CacheService<T extends FintLinks & Serializable> {

    private final ConsumerConfig<T> consumerConfig;
    private final Cache<T> cache;
    private final CacheManager cacheManager;
    private final KafkaService<T> kafkaService;

    public CacheService(ConsumerConfig<T> consumerConfig,
                        CacheManager cacheManager,
                        KafkaService<T> kafkaService) {
        this.consumerConfig = consumerConfig;
        this.cacheManager = cacheManager;
        this.kafkaService = kafkaService;

        cache = initializeCache(cacheManager, consumerConfig, consumerConfig.getResourceName());
    }

    protected abstract Cache<T> initializeCache(CacheManager cacheManager, ConsumerConfig<T> consumerConfig, String modelName);

    public abstract Optional<T> getBySystemId(String systemId);

    protected Cache<T> getCache() {
        return cache;
    }

    public long getLastUpdated() {
        return cache.getLastUpdated();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public void resetCache() {
        cache.flush();
        kafkaService.getEntityKafkaConsumer().seekToBeginning();
    }

    public Stream<T> streamSliceSince(long sinceTimeStamp, int offset, int size) {
        return null;
    }

    public Stream<T> streamSlice(int offset, int size) {
        return null;
    }

    public Stream<T> streamSince(long sinceTimeStamp) {
        return cache.streamSince(sinceTimeStamp);
    }

    public Stream<T> streamAll() {
        return cache.stream();
    }

    public Stream<T> streamByHashCode(int hashCode) {
        return cache.streamByHashCode(hashCode);
    }

    public String getCacheUrn() {
        return cache.getUrn();
    }

    public String getResourceName() {
        return consumerConfig.getResourceName();
    }
}