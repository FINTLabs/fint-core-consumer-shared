package no.fintlabs.core.consumer.shared.resource.event;

import jakarta.annotation.PostConstruct;
import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Serializable;

public abstract class EventRequestKafkaConsumer<T extends FintLinks & Serializable> {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final ConsumerConfig<T> consumerConfig;
    private final EventCache<RequestFintEvent> eventRequestCache;

    public EventRequestKafkaConsumer(EventConsumerFactoryService eventConsumerFactoryService,
                                     ConsumerConfig<T> consumerConfig) {
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
        return "%s-%s-%s-%s-%s".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType == OperationType.CREATE ? "create" : "update",
                "request");
    }

}
