package ru.practicum.ewm.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserAction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.messages.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.messages.RecommendedEventProto;
import ru.practicum.ewm.stats.messages.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.messages.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class RecommendationsHandlerImpl implements RecommendationsHandler{
    private final UserActionRepository actionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {

        Long userId = request.getUserId();
        int limit = request.getMaxResults();

        // Получаем последние действия пользователя.
        // Важно: делаем один запрос вместо множества запросов внутри циклов.
        List<UserAction> userActions = actionRepository.findAllByUserId(
                userId,
                PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Direction.DESC, "timestamp")
                )
        );

        // Если пользователь еще ничего не делал,
        // рекомендации построить невозможно.
        if (userActions.isEmpty()) {
            return List.of();
        }

        // Сохраняем ID событий, с которыми пользователь уже взаимодействовал.
        // Set используется для быстрого поиска O(1).
        Set<Long> seenEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // Сохраняем веса действий пользователя.
        // Далее они понадобятся для вычисления итогового рейтинга рекомендации.
        Map<Long, Double> actionWeights = userActions.stream()
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getWeight,
                        Math::max
                ));

        // Находим события, похожие на те, которые пользователь уже видел.
        // Выполняется один запрос в БД.
        List<EventSimilarity> candidateSimilarities =
                similarityRepository.findAllByEventAInOrEventBIn(
                        seenEventIds,
                        seenEventIds,
                        PageRequest.of(
                                0,
                                limit * 2,
                                Sort.by(Sort.Direction.DESC, "score")
                        )
                );

        // Из найденных связей собираем события-кандидаты.
        // Исключаем события, которые пользователь уже видел.
        Set<Long> candidateIds = candidateSimilarities.stream()
                .flatMap(similarity ->
                        Stream.of(
                                similarity.getEventA(),
                                similarity.getEventB()
                        )
                )
                .filter(id -> !seenEventIds.contains(id))
                .collect(Collectors.toSet());

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // Получаем все связи между кандидатами и уже просмотренными событиями.
        // Раньше здесь выполнялись запросы внутри цикла.
        // Теперь выполняется один запрос.
        List<EventSimilarity> relevantSimilarities =
                similarityRepository.findAllByEventAInOrEventBIn(
                        candidateIds,
                        seenEventIds,
                        PageRequest.of(
                                0,
                                limit * 5,
                                Sort.by(Sort.Direction.DESC, "score")
                        )
                );

        // Для каждого кандидата будем накапливать:
        // 1. сумму score * weight
        // 2. сумму score
        Map<Long, ScoreAccumulator> scores = new HashMap<>();

        for (EventSimilarity similarity : relevantSimilarities) {

            Long candidateId;
            Long seenEventId;

            // Определяем, какая сторона является кандидатом,
            // а какая относится к уже просмотренным пользователем событиям.
            if (candidateIds.contains(similarity.getEventA())
                    && seenEventIds.contains(similarity.getEventB())) {

                candidateId = similarity.getEventA();
                seenEventId = similarity.getEventB();

            } else if (candidateIds.contains(similarity.getEventB())
                    && seenEventIds.contains(similarity.getEventA())) {

                candidateId = similarity.getEventB();
                seenEventId = similarity.getEventA();

            } else {
                continue;
            }

            double score = similarity.getScore();

            // Получаем вес пользовательского действия.
            // Если по какой-то причине вес отсутствует,
            // используем значение по умолчанию.
            double weight = actionWeights.getOrDefault(seenEventId, 1.0);

            ScoreAccumulator accumulator = scores.computeIfAbsent(
                    candidateId,
                    id -> new ScoreAccumulator()
            );

            accumulator.add(score, weight);
        }

        // Формируем итоговый список рекомендаций.
        return scores.entrySet()
                .stream()
                .map(entry ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(entry.getKey())
                                .setScore(entry.getValue().getFinalScore())
                                .build()
                )
                .sorted(
                        Comparator.comparing(
                                        RecommendedEventProto::getScore)
                                .reversed()
                )
                .limit(limit)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {

        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int limit = request.getMaxResults();

        // Получаем все события, которые пользователь уже видел.
        // Это позволяет избежать запросов exists(...) внутри цикла.
        Set<Long> seenEventIds =
                actionRepository.findAllEventIdsByUserId(userId);

        // Получаем все связи для указанного события.
        List<EventSimilarity> similarities =
                similarityRepository.findAllByEventAOrEventB(
                        eventId,
                        eventId,
                        PageRequest.of(
                                0,
                                limit * 2,
                                Sort.by(Sort.Direction.DESC, "score")
                        )
                );

        return similarities.stream()

                // Определяем второе событие в паре.
                .map(similarity -> {

                    Long relatedEventId =
                            similarity.getEventA().equals(eventId)
                                    ? similarity.getEventB()
                                    : similarity.getEventA();

                    return RecommendedEventProto.newBuilder()
                            .setEventId(relatedEventId)
                            .setScore(similarity.getScore())
                            .build();
                })

                // Исключаем события, которые пользователь уже видел.
                .filter(proto -> !seenEventIds.contains(proto.getEventId()))

                // Убираем возможные дубликаты.
                // Если нашли одинаковый eventId,
                // оставляем максимальный score.
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore,
                        Math::max
                ))

                .entrySet()
                .stream()

                .map(entry ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(entry.getKey())
                                .setScore(entry.getValue())
                                .build()
                )

                .sorted(
                        Comparator.comparing(
                                        RecommendedEventProto::getScore)
                                .reversed()
                )
                .limit(limit)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(
            InteractionsCountRequestProto request
    ) {

        List<Long> eventIds = request.getEventIdList();

        if (eventIds.isEmpty()) {
            return List.of();
        }

        // Получаем агрегированную статистику по всем событиям
        // одним запросом вместо отдельных запросов для каждого ID.
        List<Object[]> results =
                actionRepository.sumWeightsByEventIds(eventIds);

        // Преобразуем результат в Map:
        // eventId -> суммарный вес взаимодействий
        Map<Long, Double> weightMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));

        return weightMap.entrySet()
                .stream()
                .map(entry ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(entry.getKey())
                                .setScore(entry.getValue())
                                .build()
                )
                .sorted(
                        Comparator.comparing(
                                        RecommendedEventProto::getScore)
                                .reversed()
                )
                .toList();
    }

    /**
     * Накапливает промежуточные значения,
     * необходимые для вычисления итогового рейтинга рекомендации.
     *
     * Формула:
     *
     * итоговый рейтинг =
     * Σ(score * weight) / Σ(score)
     */
    private static class ScoreAccumulator {

        private double weightedScoreSum;

        private double similarityScoreSum;

        void add(double similarityScore, double weight) {
            weightedScoreSum += similarityScore * weight;
            similarityScoreSum += similarityScore;
        }

        double getFinalScore() {
            return similarityScoreSum == 0
                    ? 0
                    : weightedScoreSum / similarityScoreSum;
        }
    }

}
