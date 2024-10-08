package no.fintlabs.core.consumer.shared.resource;

import no.fint.model.resource.FintLinks;
import no.fintlabs.cache.Cache;
import no.fintlabs.cache.CacheManager;
import no.fintlabs.core.consumer.shared.resource.kafka.EntityKafkaConsumer;
import no.fintlabs.core.consumer.shared.resource.kafka.KafkaEventLogger;
import org.apache.kafka.common.header.Header;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class CacheService<T extends FintLinks & Serializable> {

    private final ConsumerConfig<T> consumerConfig;
    private final Cache<T> cache;
    private final CacheManager cacheManager;
    private final EntityKafkaConsumer<T> entityKafkaConsumer;
    protected final KafkaEventLogger eventLogger;

    public CacheService(ConsumerConfig<T> consumerConfig,
                        CacheManager cacheManager,
                        EntityKafkaConsumer<T> entityKafkaConsumer) {
        this.consumerConfig = consumerConfig;
        this.cacheManager = cacheManager;
        this.entityKafkaConsumer = entityKafkaConsumer;
        cache = initializeCache(cacheManager, consumerConfig, consumerConfig.getResourceName());
        this.eventLogger = new KafkaEventLogger(consumerConfig.getResourceName(), cache);
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
        entityKafkaConsumer.seekToBeginning();
    }

    public Stream<T> streamSliceSince(long sinceTimeStamp, int offset, int size) {
        return cache.streamSliceSince(sinceTimeStamp, offset, size);
    }

    public Stream<T> streamSlice(int offset, int size) {
        return cache.streamSlice(offset, size);
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

    private byte[] previousRetentionTimeValue = null;

    protected void updateRetensionTime(Header header) {
        if (header != null) {
            byte[] currentRetentionTimeValue = header.value();
            if (!Arrays.equals(previousRetentionTimeValue, currentRetentionTimeValue)) {
                previousRetentionTimeValue = currentRetentionTimeValue;
                long retensionTime = convertRetensionTime(header.value());
                if (retensionTime != entityKafkaConsumer.getTopicRetensionTime()) {
                    entityKafkaConsumer.setTopicRetensionTime(retensionTime);
                    getCache().setRetentionPeriodInMs(retensionTime);
                }
            }
        }
    }

    private long convertRetensionTime(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(value);
        buffer.flip();
        return buffer.getLong();
    }
}
