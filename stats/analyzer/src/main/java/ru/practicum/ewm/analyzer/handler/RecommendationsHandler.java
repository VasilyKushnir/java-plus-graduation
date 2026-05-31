package ru.practicum.ewm.analyzer.handler;

import ru.practicum.ewm.stats.messages.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.messages.RecommendedEventProto;
import ru.practicum.ewm.stats.messages.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.messages.UserPredictionsRequestProto;

import java.util.List;

public interface RecommendationsHandler {
    List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}
