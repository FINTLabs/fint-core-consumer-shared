package no.fintlabs.core.consumer.shared.resource.kafka;

import lombok.Getter;
import no.fint.model.resource.FintLinks;

import java.io.Serializable;

public class KafkaService<T extends FintLinks & Serializable> {

    @Getter
    private final EntityKafkaConsumer<T> entityKafkaConsumer;

    @Getter
    private final EventKafkaProducer<T> eventKafkaProducer;

    public KafkaService(EntityKafkaConsumer<T> entityKafkaConsumer, EventKafkaProducer<T> eventKafkaProducer) {
        this.entityKafkaConsumer = entityKafkaConsumer;
        this.eventKafkaProducer = eventKafkaProducer;
    }
}
