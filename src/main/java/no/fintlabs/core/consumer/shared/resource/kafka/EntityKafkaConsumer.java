package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.ListenerBeanRegistrationService;
import no.fintlabs.kafka.common.OffsetSeekingTrigger;
import no.fintlabs.kafka.entity.EntityConsumerConfiguration;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
public class EntityKafkaConsumer<V> {

    private final EntityConsumerFactoryService entityConsumerFactoryService;
    private final ListenerBeanRegistrationService listenerBeanRegistrationService;
    private final EntityTopicService entityTopicService;
    private final String resourceName;
    private final OffsetSeekingTrigger resetTrigger;

    public EntityKafkaConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ListenerBeanRegistrationService listenerBeanRegistrationService,
            EntityTopicService entityTopicService,
            ConsumerConfig consumerConfig
    ) {
        this.entityConsumerFactoryService = entityConsumerFactoryService;
        this.listenerBeanRegistrationService = listenerBeanRegistrationService;
        this.entityTopicService = entityTopicService;
        this.resourceName = String.format("%s-%s-%s",
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName()
        );
        resetTrigger = new OffsetSeekingTrigger();
    }

    public long registerListener(Class<V> clazz, Consumer<ConsumerRecord<String, V>> consumer) {
        EntityTopicNameParameters topicNameParameters = EntityTopicNameParameters
                .builder()
                .resource(resourceName)
                .build();

        entityTopicService.ensureTopic(topicNameParameters, 0);

        long retention = getRetention(topicNameParameters);
        // TODO: 11/03/2022 What to do if fails to get retention
        // TODO: 11/05/2022 What if the adapter re-register and the retention change

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

        log.info("Listening to topic: " + topicNameParameters.getOrgId() + "." + topicNameParameters.getDomainContext() + "." + topicNameParameters.getResource());
        listenerBeanRegistrationService.registerBean(messageListenerContainer);

        return retention;
    }

    public void seekToBeginning() {
        resetTrigger.seekToBeginning();
    }

    private long getRetention(EntityTopicNameParameters topicNameParameters) {
        try {
            Map<String, String> config = entityTopicService.getTopicConfig(topicNameParameters);
            String output = config.get(TopicConfig.RETENTION_MS_CONFIG);
            return Long.parseLong(output);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
