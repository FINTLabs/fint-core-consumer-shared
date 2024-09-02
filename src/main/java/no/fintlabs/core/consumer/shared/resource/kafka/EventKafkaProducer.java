package no.fintlabs.core.consumer.shared.resource.kafka;

import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.resource.ConsumerConfig;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public abstract class EventKafkaProducer {

    private final EventProducer<Object> eventProducer;
    private final ConsumerConfig<?> consumerConfig;


    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig<?> consumerConfig) {
        this.consumerConfig = consumerConfig;
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
    }

    public void sendEvent(RequestFintEvent event) {
        String eventName = createEventName();

        EventTopicNameParameters topicNameParameters = EventTopicNameParameters
                .builder()
                .orgId(consumerConfig.getOrgId())
                .domainContext("fint-core")
                .eventName(eventName)
                .build();

        eventProducer.send(
                EventProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(event)
                        .build()
        );
    }

    private String createEventName() {
        return "%s-%s-%s-request".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName()
        );
    }

}
