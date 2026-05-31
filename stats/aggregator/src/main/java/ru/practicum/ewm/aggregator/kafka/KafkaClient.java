package ru.practicum.ewm.aggregator.kafka;

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
import ru.practicum.ewm.serialization.deserializer.UserActionAvroDeserializer;
import ru.practicum.ewm.serialization.serializer.GeneralAvroSerializer;

import java.time.Duration;
import java.util.Properties;

@Getter
@Component
public class KafkaClient {
    private final Producer<Long, SpecificRecordBase> producer;
    private final Consumer<Long, SpecificRecordBase> consumer;

    public KafkaClient(
            @Value("${application.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${application.kafka.consumer.group-id}") String consumerGroupId
    ) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);
        producer = new KafkaProducer<>(producerConfig);

        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getCanonicalName());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumer = new KafkaConsumer<>(consumerConfig);
    }

    @PreDestroy
    public void closeConnection() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
