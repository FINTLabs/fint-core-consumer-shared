package no.fintlabs.core.consumer.shared.resource.event;

import jakarta.annotation.PostConstruct;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Serializable;
import java.time.Duration;

public abstract class EventResponseKafkaConsumer<T extends FintLinks & Serializable> {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final ConsumerConfig<T> consumerConfig;
    private final EventCache<ResponseFintEvent<T>> eventResponseCache;
    private final EventEntityCache<T> eventEntityCache;
    private final EventTopicService eventTopicService;

    public EventResponseKafkaConsumer(EventConsumerFactoryService eventConsumerFactoryService,
                                      ConsumerConfig<T> consumerConfig, FintLinker<T> fintLinker,
                                      EventTopicService eventTopicService) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        this.consumerConfig = consumerConfig;
        this.eventTopicService = eventTopicService;
        eventResponseCache = new EventCache<>();
        eventEntityCache = new EventEntityCache<T>(fintLinker);
    }

    public EventCache<ResponseFintEvent<T>> getCache() {
        return eventResponseCache;
    }

    public EventEntityCache<T> getEntityCache() {
        return eventEntityCache;
    }

    @PostConstruct
    private void init() {
        EventTopicNamePatternParameters eventTopicName = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.anyOf(consumerConfig.getOrgId()))
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.anyOf(getEventName()))
                .build();

        eventConsumerFactoryService.createFactory(
                ResponseFintEvent.class,
                this::consumeEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .build()
        ).createContainer(eventTopicName);

    }

    private void consumeEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        eventResponseCache.add(consumerRecord.value());
    }

    private String getEventName() {
        return "%s-%s-%s-response".formatted(consumerConfig.getDomainName(), consumerConfig.getPackageName(), consumerConfig.getResourceName());
    }

}
