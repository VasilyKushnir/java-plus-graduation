package ru.practicum.ewm.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.collector.kafka.KafkaClient;
import ru.practicum.ewm.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.messages.UserActionProto;

@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    private final KafkaClient kafkaClient;

    @Value("${application.kafka.topics.user-actions}")
    private String userActionTopic;

    @Override
    public void collectUserAction(UserActionProto request) {
        UserActionAvro actionAvro = UserActionMapper.mapToAvro(request);
        kafkaClient.send(userActionTopic, actionAvro, actionAvro.getTimestamp(), actionAvro.getEventId());
    }
}
