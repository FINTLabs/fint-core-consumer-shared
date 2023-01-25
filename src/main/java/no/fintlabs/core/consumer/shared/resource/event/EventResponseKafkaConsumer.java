package no.fintlabs.core.consumer.shared.resource.event;

import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.core.consumer.shared.resource.kafka.EventKafkaProducer;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.PostConstruct;

public abstract class EventResponseKafkaConsumer {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final ConsumerConfig consumerConfig;
    private final EventResponseCacheService eventResponseCacheService;

    public EventResponseKafkaConsumer(EventConsumerFactoryService eventConsumerFactoryService, ConsumerConfig consumerConfig) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        this.consumerConfig = consumerConfig;
        this.eventResponseCacheService = new EventResponseCacheService();
    }

    public EventResponseCacheService getCache() {
        return eventResponseCacheService;
    }

    @PostConstruct
    private void init() {
        EventTopicNamePatternParameters topicPatternParameter = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.anyOf(consumerConfig.getOrgId()))
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.anyOf(
                        createEventName(EventKafkaProducer.OperationType.CREATE),
                        createEventName(EventKafkaProducer.OperationType.UPDATE)
                ))
                .build();

        eventConsumerFactoryService.createFactory(
                ResponseFintEvent.class,
                this::consumeEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .build()
        ).createContainer(topicPatternParameter);

    }

    private void consumeEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        eventResponseCacheService.add(consumerRecord.value());
    }

    private String createEventName(EventKafkaProducer.OperationType operationType) {
        return String.format("%s-%s-%s-%s-%s",
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType == EventKafkaProducer.OperationType.CREATE ? "create" : "update",
                "response");
    }

}
