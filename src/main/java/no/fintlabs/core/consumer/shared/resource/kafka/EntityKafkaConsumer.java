package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.ListenerBeanRegistrationService;
import no.fintlabs.kafka.common.OffsetSeekingTrigger;
import no.fintlabs.kafka.entity.EntityConsumerConfiguration;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.function.Consumer;

@Slf4j
public abstract class EntityKafkaConsumer<V> {

    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ListenerBeanRegistrationService listenerBeanRegistrationService;
    private final String resourceName;
    private final OffsetSeekingTrigger resetTrigger;

    @Getter
    private Long topicRetensionTime = 0L;

    public EntityKafkaConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ListenerBeanRegistrationService listenerBeanRegistrationService,
            ConsumerConfig<?> consumerConfig
    ) {
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.listenerBeanRegistrationService = listenerBeanRegistrationService;
        this.resourceName = "%s-%s-%s".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName()
        );
        resetTrigger = new OffsetSeekingTrigger();
    }

    public void registerListener(Class<V> clazz, Consumer<ConsumerRecord<String, V>> consumer) {
        EntityTopicNameParameters topicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(resourceName)
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

        log.info("Listening to topic: " + topicNameParameters.getTopicName());
        listenerBeanRegistrationService.registerBean(messageListenerContainer);
    }

    public void seekToBeginning() {
        resetTrigger.seekToBeginning();
    }

    public void setTopicRetensionTime(Long topicRetensionTime) {
        log.info("Setting retension time for {} to {}", resourceName, topicRetensionTime);
        this.topicRetensionTime = topicRetensionTime;
    }
}
