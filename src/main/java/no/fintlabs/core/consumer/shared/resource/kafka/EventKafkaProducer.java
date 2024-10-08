package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public abstract class EventKafkaProducer {

    private final EventProducer<Object> eventProducer;
    private final ConsumerConfig<?> consumerConfig;
    private final EventTopicService eventTopicService;
    private final Set<String> existingTopics;


    public EventKafkaProducer(EventProducerFactory eventProducerFactory, ConsumerConfig<?> consumerConfig, EventTopicService eventTopicService) {
        this.consumerConfig = consumerConfig;
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
        this.eventTopicService = eventTopicService;
        this.existingTopics = new HashSet<>();
    }

    public void sendEvent(RequestFintEvent event) {
        String eventName = createEventName();

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

    private String createEventName() {
        return "%s-%s-%s-request".formatted(
                consumerConfig.getDomainName(),
                consumerConfig.getPackageName(),
                consumerConfig.getResourceName()
        );
    }

    private void createTopicIfNotExists(String eventName, EventTopicNameParameters topicNameParameters) {
        if (!existingTopics.contains(eventName)) {
            log.info("Ensuring event topic: {}", topicNameParameters.getTopicName());
            eventTopicService.ensureTopic(topicNameParameters, Duration.ofDays(2).toMillis());
            existingTopics.add(eventName);
        }
    }
}
