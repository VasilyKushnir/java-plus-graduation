package ru.practicum.ewm.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.analyzer.kafka.KafkaClient;
import ru.practicum.ewm.analyzer.service.AnalyzerService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarter implements CommandLineRunner {
    private final KafkaClient kafkaClient;
    private final AnalyzerService analyzerService;

    @Value("${application.kafka.topics.user-actions}")
    private String topicUserActions;
    @Value("${application.kafka.topics.events-similarity}")
    private String topicEventsSimilarity;

    @Override
    public void run(String... args) {
        Thread userActionThread = new Thread(this::processUserActions, "user-action");
        userActionThread.setDaemon(true);

        Thread eventSimilarityThread = new Thread(this::processEventSimilarities, "event-similarity");
        eventSimilarityThread.setDaemon(true);

        userActionThread.start();
        eventSimilarityThread.start();
    }

    private void processUserActions() {
        try (Consumer<Long, UserActionAvro> consumer = kafkaClient.getActionConsumer()) {
            consumer.subscribe(List.of(topicUserActions));

            log.info("Starting poll loop for UserAction topic.");
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    continue;
                }

                log.info("Received {} UserAction records.", records.count());
                try {
                    for (var record : records) {
                        analyzerService.analyzeUserAction(record.value());
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("Failed to process UserAction", e);
                }
            }
        } catch (Exception e) {
            log.error("Fatal error in UserAction consumer", e);
        } finally {
            log.warn("UserAction Consumer thread stopped");
        }
    }

    /**
     * Main processing loop for EventSimilarity records.
     */
    private void processEventSimilarities() {
        try (Consumer<Long, EventSimilarityAvro> consumer = kafkaClient.getSimilarityConsumer()) {

            consumer.subscribe(List.of(topicEventsSimilarity));

            log.info("Starting poll loop for EventSimilarity topic.");
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    continue;
                }

                log.info("Received {} EventSimilarity records.", records.count());
                try {
                    for (var record : records) {
                        analyzerService.analyzeEventSimilarity(record.value());
                    }
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("Failed to process EventSimilarity", e);
                }
            }
        } catch (Exception e) {
            log.error("Fatal error in EventSimilarity consumer", e);
        } finally {
            // Clean up resources when the thread exits
            log.warn("EventSimilarity Consumer thread stopped");
        }
    }
}