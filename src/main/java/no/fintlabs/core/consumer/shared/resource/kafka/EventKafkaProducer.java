package no.fintlabs.core.consumer.shared.resource.kafka;

import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;

public class EventKafkaProducer {

    private final EventProducer<Object> eventProducer;

    private final ConsumerConfig consumerConfig;

    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
    }

    public void sendEvent(RequestFintEvent<?> event, OperationType operationType) {

        String eventName = String.format("%s-%s-%s-%s-%s",
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType.equals(OperationType.CREATE) ? "create" : "update",
                "request");

        eventProducer.send(
                EventProducerRecord.<Object>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(consumerConfig.getOrgId())
                                .domainContext("fint-core")
                                .eventName(eventName)
                                .build())
                        .value(event)
                        .build()
        );
    }

    public enum OperationType {
        CREATE,
        UPDATE
    }
}
