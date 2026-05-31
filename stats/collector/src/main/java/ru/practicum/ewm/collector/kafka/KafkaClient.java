package ru.practicum.ewm.collector.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.serialization.serializer.GeneralAvroSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

@Component
public class KafkaClient {
    Producer<Long, SpecificRecordBase> producer;

    public KafkaClient(@Value("${application.kafka.bootstrap-servers}")
                       String bootstrapServers) {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);

        producer = new KafkaProducer<>(config);
    }

    public void send(String topic, SpecificRecordBase record, Instant timestamp, Long key) {
        ProducerRecord<Long, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(topic, null, timestamp.toEpochMilli(), key, record);
        producer.send(producerRecord);
    }

    @PreDestroy
    public void closeConnection() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
