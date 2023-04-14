package no.fintlabs.core.consumer.shared.resource.event;

import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.PostConstruct;

public abstract class EventRequestKafkaConsumer {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final ConsumerConfig<?> consumerConfig;
    private final EventCache<RequestFintEvent> eventRequestCache;

    public EventRequestKafkaConsumer(EventConsumerFactoryService eventConsumerFactoryService,
                                     ConsumerConfig<?> consumerConfig) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        this.consumerConfig = consumerConfig;
        this.eventRequestCache = new EventCache<>();
    }

    public EventCache<RequestFintEvent> getCache() {
        return eventRequestCache;
    }

    @PostConstruct
    private void init() {
        EventTopicNamePatternParameters topicPatternParameter = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.anyOf(consumerConfig.getOrgId()))
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.anyOf(
                        createEventName(OperationType.CREATE),
                        createEventName(OperationType.UPDATE)
                ))
                .build();

        eventConsumerFactoryService.createFactory(
                RequestFintEvent.class,
                this::consumeEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .build()
        ).createContainer(topicPatternParameter);

    }

    private void consumeEvent(ConsumerRecord<String, RequestFintEvent> consumerRecord) {
        eventRequestCache.add(consumerRecord.value());
    }

    private String createEventName(OperationType operationType) {
        return String.format("%s-%s-%s-%s-%s",
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType == OperationType.CREATE ? "create" : "update",
                "request");
    }

}
