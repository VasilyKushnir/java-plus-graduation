package ru.practicum.ewm.aggregator;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.aggregator.kafka.KafkaClient;
import ru.practicum.ewm.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private final AggregatorService aggregatorService;
    private final KafkaClient kafkaClient;

    @Value("${application.kafka.topics.user-actions}")
    private String userActionsTopic;
    @Value("${application.kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    public void start() {
        Consumer<Long, SpecificRecordBase> consumer = kafkaClient.getConsumer();
        Producer<Long, SpecificRecordBase> producer = kafkaClient.getProducer();

        try {
            consumer.subscribe(List.of(userActionsTopic));
            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    List<EventSimilarityAvro> eventsSimilarity = aggregatorService.aggregateUserAction(record.value());
                    if (!eventsSimilarity.isEmpty()) {
                        for (EventSimilarityAvro eventSimilarity : eventsSimilarity) {
                            producer.send(new ProducerRecord<>(
                                    eventsSimilarityTopic,
                                    null,
                                    eventSimilarity.getTimestamp().toEpochMilli(),
                                    eventSimilarity.getEventA(),
                                    eventSimilarity
                            ));
                        }
                    }
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignore) {
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}
