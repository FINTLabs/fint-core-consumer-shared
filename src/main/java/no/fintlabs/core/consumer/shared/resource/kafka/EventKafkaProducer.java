package no.fintlabs.core.consumer.shared.resource.kafka;

import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;

import java.util.ArrayList;
import java.util.List;

public abstract class EventKafkaProducer {

    private static final int RETENTION_TIME_MS = 172800000;

    private final EventProducer<Object> eventProducer;

    private final ConsumerConfig<?> consumerConfig;

    private final EventTopicService eventTopicService;

    private final List<String> existingTopics;


    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig<?> consumerConfig, EventTopicService eventTopicService) {
        this.consumerConfig = consumerConfig;
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
        this.eventTopicService = eventTopicService;
        this.existingTopics = new ArrayList<>();
    }

    public void sendEvent(RequestFintEvent event, OperationType operationType) {

        String eventName = "%s-%s-%s-%s-%s".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType.equals(OperationType.CREATE) ? "create" : "update",
                "request");

        EventTopicNameParameters topicNameParameters = EventTopicNameParameters
                .builder()
                .orgId(consumerConfig.getOrgId())
                .domainContext("fint-core")
                .eventName(eventName)
                .build();

        createTopicIfNotExists(eventName, topicNameParameters);

        eventProducer.send(
                EventProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(event)
                        .build()
        );
    }

    private void createTopicIfNotExists(String eventName, EventTopicNameParameters topicNameParameters) {
        if (!existingTopics.contains(eventName)) {
            eventTopicService.ensureTopic(topicNameParameters, RETENTION_TIME_MS);
            existingTopics.add(eventName);
        }
    }
}
