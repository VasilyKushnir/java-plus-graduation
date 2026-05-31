package ru.practicum.ewm.analyzer.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface AnalyzerService {
    void analyzeUserAction(UserActionAvro action);

    void analyzeEventSimilarity(EventSimilarityAvro similarity);
}
