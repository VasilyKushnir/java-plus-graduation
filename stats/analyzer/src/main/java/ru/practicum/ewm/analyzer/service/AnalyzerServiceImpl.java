package ru.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.ewm.analyzer.mapper.UserActionMapper;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserAction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnalyzerServiceImpl implements AnalyzerService {
    private final EventSimilarityRepository similarityRepository;
    private final UserActionRepository actionRepository;
    private final UserActionMapper userActionMapper;

    @Override
    public void analyzeEventSimilarity(EventSimilarityAvro similarityAvro) {
        EventSimilarity similarity = EventSimilarityMapper.mapToEventSimilarity(similarityAvro);

        if (!similarityRepository.existsByEventAAndEventB(similarity.getEventA(), similarity.getEventB())) {
            similarityRepository.save(similarity);
        } else {
            EventSimilarity oldSimilarity = similarityRepository
                    .findByEventAAndEventB(similarity.getEventA(), similarity.getEventB()).get();

            oldSimilarity.setScore(similarity.getScore());
            oldSimilarity.setTimestamp(similarity.getTimestamp());
            similarityRepository.save(oldSimilarity);
        }
    }

    @Override
    public void analyzeUserAction(UserActionAvro actionAvro) {
        UserAction action = userActionMapper.mapToUserAction(actionAvro);

        if (!actionRepository.existsByEventIdAndUserId(action.getEventId(), action.getUserId())) {
            actionRepository.save(action);
        } else {
            UserAction oldAction = actionRepository
                    .findByEventIdAndUserId(action.getEventId(), action.getUserId()).get();

            if (action.getWeight() > oldAction.getWeight()) {
                oldAction.setWeight(action.getWeight());
                oldAction.setTimestamp(action.getTimestamp());
                actionRepository.save(oldAction);
            }
        }
    }
}