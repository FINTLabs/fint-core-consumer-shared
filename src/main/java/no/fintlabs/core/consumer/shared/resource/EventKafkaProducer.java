package no.fintlabs.core.consumer.shared.resource;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.ConsumerProps;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;

import java.io.Serializable;

public class EventKafkaProducer<T extends FintLinks & Serializable> {

    private final EventProducer<RequestFintEvent<T>> eventProducer;

    private final ConsumerProps consumerProps;

    public EventKafkaProducer(EventProducerFactory eventProducerFactory, Class<RequestFintEvent<T>> clazz, ConsumerProps consumerProps) {
        this.consumerProps = consumerProps;
        eventProducer = eventProducerFactory.createProducer(clazz);
    }

    public void sendEvent(RequestFintEvent<T> event, OperationType operationType) {

        String eventName = String.format("{}-{}-{}-{}-{}",
                consumerProps.getDomainName(),
                consumerProps.getPackageName(),
                consumerProps.getResourceName(),
                operationType.equals(OperationType.CREATE) ? "create" : "update",
                "request");

        eventProducer.send(
                EventProducerRecord.<RequestFintEvent<T>>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(consumerProps.getOrgId())
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
