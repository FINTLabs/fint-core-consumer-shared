package no.fintlabs.core.consumer.shared.resource.kafka;

import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;

public abstract class EventKafkaProducer {

    private static final int RETENTION_TIME_MS = 172800000;

    private final EventProducer<Object> eventProducer;

    private final ConsumerConfig consumerConfig;

    private final EventTopicService eventTopicService;

    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig consumerConfig, EventTopicService eventTopicService) {
        this.consumerConfig = consumerConfig;
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
        this.eventTopicService = eventTopicService;
    }

    public void sendEvent(RequestFintEvent event, OperationType operationType) {

        String eventName = String.format("%s-%s-%s-%s-%s",
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

        eventTopicService.ensureTopic(topicNameParameters, RETENTION_TIME_MS);

        eventProducer.send(
                EventProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(event)
                        .build()
        );
    }

    public enum OperationType {
        CREATE,
        UPDATE
    }
}
