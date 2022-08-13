package no.fintlabs.core.consumer.shared.resource.kafka;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;

import java.io.Serializable;

public class EventKafkaProducer<T extends FintLinks & Serializable> {

    private final EventProducer<RequestFintEvent<T>> eventProducer;

    private final ConsumerConfig consumerConfig;

    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
        eventProducer = eventProducerFactory.createProducer(consumerConfig.getReqestFintEventClass());
    }

    public void sendEvent(RequestFintEvent<T> event, OperationType operationType) {

        String eventName = String.format("{}-{}-{}-{}-{}",
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName(),
                operationType.equals(OperationType.CREATE) ? "create" : "update",
                "request");

        eventProducer.send(
                EventProducerRecord.<RequestFintEvent<T>>builder()
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
