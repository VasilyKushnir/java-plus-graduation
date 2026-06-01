package ru.practicum.ewm.analyzer.kafka;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.serialization.deserializer.EventSimilarityAvroDeserializer;
import ru.practicum.ewm.serialization.deserializer.UserActionAvroDeserializer;
import ru.practicum.ewm.serialization.serializer.GeneralAvroSerializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.Properties;

@Getter
@Component
public class KafkaClient {
    private final Producer<Long, SpecificRecordBase> producer;
    private final Consumer<Long, UserActionAvro> actionConsumer;
    private final Consumer<Long, EventSimilarityAvro> similarityConsumer;

    public KafkaClient(
            @Value("${application.kafka.consumer.bootstrap-servers}")
            String bootstrapServers,
            @Value("${application.kafka.consumer.group-id.user-action}")
            String consumerGroupIdUserAction,
            @Value("${application.kafka.consumer.group-id.event-similarity}")
            String consumerGroupIdEventSimilarity
    ) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);
        producer = new KafkaProducer<>(producerConfig);

        Properties actionConsumerConfig = new Properties();
        actionConsumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupIdUserAction);
        actionConsumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        actionConsumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getCanonicalName());
        actionConsumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        actionConsumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        actionConsumer = new KafkaConsumer<>(actionConsumerConfig);

        Properties similarityConsumerConfig = new Properties();
        similarityConsumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupIdEventSimilarity);
        similarityConsumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        similarityConsumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getCanonicalName());
        similarityConsumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class);
        similarityConsumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        similarityConsumer = new KafkaConsumer<>(similarityConsumerConfig);
    }

    @PreDestroy
    public void closeConnection() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
