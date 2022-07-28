package no.fintlabs.core.consumer.shared;

import no.fintlabs.cache.Cache;
import no.fintlabs.cache.CacheManager;

import java.io.Serializable;
import java.util.stream.Stream;

// TODO: 01/03/2022 Move to fint-core-cache
public abstract class ConsumerService<T extends Serializable> {

    private Cache<T> cache;
    private CacheManager cacheManager;
    private final ConsumerProps consumerProps;
    private final String modelName;
    private final KafkaConsumer<T> kafkaConsumer;

    public ConsumerService(CacheManager cacheManager, Class modelType, ConsumerProps consumerProps, KafkaConsumer<T> kafkaConsumer) {
        this.cacheManager = cacheManager;
        this.consumerProps = consumerProps;
        this.modelName = modelType.getSimpleName().toLowerCase();
        this.kafkaConsumer = kafkaConsumer;

        cache = initializeCache(cacheManager, consumerProps, modelName);
    }

    protected abstract Cache<T> initializeCache(CacheManager cacheManager, ConsumerProps consumerProps, String modelName);

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
        kafkaConsumer.seekToBeginning();
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

    public String getModelName() {
        return modelName;
    }
}
