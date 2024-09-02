package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.ListenerBeanRegistrationService;
import no.fintlabs.kafka.common.OffsetSeekingTrigger;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.entity.EntityConsumerConfiguration;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.function.Consumer;

@Slf4j
public abstract class EntityKafkaConsumer<V> {

    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ListenerBeanRegistrationService listenerBeanRegistrationService;
    private final OffsetSeekingTrigger resetTrigger;
    private final ConsumerConfig<?> consumerConfig;

    @Getter
    private Long topicRetensionTime = 0L;

    public EntityKafkaConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ListenerBeanRegistrationService listenerBeanRegistrationService,
            ConsumerConfig<?> consumerConfig
    ) {
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.listenerBeanRegistrationService = listenerBeanRegistrationService;
        this.consumerConfig = consumerConfig;
        resetTrigger = new OffsetSeekingTrigger();
    }

    public void registerListener(Class<V> clazz, Consumer<ConsumerRecord<String, V>> consumer) {
        EntityTopicNamePatternParameters topicNameParameters = EntityTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.anyOf())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .resource(FormattedTopicComponentPattern.anyOf(getResourceName()))
                .build();

        ConcurrentMessageListenerContainer<String, V> messageListenerContainer =
                entityConsumerFactoryService
                        .createFactory(
                                clazz,
                                consumer,
                                EntityConsumerConfiguration
                                        .builder()
                                        .errorHandler(new CommonLoggingErrorHandler())
                                        .offsetSeekingTrigger(resetTrigger)
                                        .build()
                        )
                        .createContainer(topicNameParameters);

        log.info("Listening to entity topic topic: {}", "%s.fint-core.entity.%s".formatted(consumerConfig.getOrgId(), getResourceName()));
        listenerBeanRegistrationService.registerBean(messageListenerContainer);
    }

    private String getResourceName() {
        return "%s-%s-%s".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName()
        );
    }

    public void seekToBeginning() {
        resetTrigger.seekToBeginning();
    }

    public void setTopicRetensionTime(Long topicRetensionTime) {
        log.info("Setting retension time for {} to {}", consumerConfig.getResourceName(), topicRetensionTime);
        this.topicRetensionTime = topicRetensionTime;
    }
}
