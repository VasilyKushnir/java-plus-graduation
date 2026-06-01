package ru.practicum.ewm.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserAction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.EventWeightProjection;
import ru.practicum.ewm.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.messages.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.messages.RecommendedEventProto;
import ru.practicum.ewm.stats.messages.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.messages.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class RecommendationsHandlerImpl implements RecommendationsHandler {

    private static final int K_NEIGHBOURS = 10;

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
                        Double::max
                ));

        // Находим события, похожие на те, которые пользователь уже видел.
        // Выполняется один запрос в БД.
        List<EventSimilarity> similarities =
                similarityRepository.findAllByEventAInOrEventBIn(
                        seenEventIds,
                        seenEventIds,
                        PageRequest.of(
                                0,
                                limit * 5,
                                Sort.by(Sort.Direction.DESC, "score")
                        )
                );

        if (similarities.isEmpty()) {
            return List.of();
        }

        /*
         * Кандидат -> список сходств с просмотренными событиями.
         *
         * Именно это соответствует замечанию ревьюера:
         * сначала отбираем кандидатов,
         * потом для каждого кандидата выбираем K ближайших соседей.
         */
        Map<Long, List<EventSimilarity>> candidateSimilarities =
                new HashMap<>();

        for (EventSimilarity similarity : similarities) {

            Long eventA = similarity.getEventA();
            Long eventB = similarity.getEventB();

            boolean aSeen = seenEventIds.contains(eventA);
            boolean bSeen = seenEventIds.contains(eventB);

            // A просмотрено -> B кандидат
            if (aSeen && !bSeen) {
                candidateSimilarities
                        .computeIfAbsent(
                                eventB,
                                id -> new ArrayList<>()
                        )
                        .add(similarity);
            }

            // B просмотрено -> A кандидат
            if (bSeen && !aSeen) {
                candidateSimilarities
                        .computeIfAbsent(
                                eventA,
                                id -> new ArrayList<>()
                        )
                        .add(similarity);
            }
        }

        // Для каждого кандидата выбираем K ближайших соседей и считаем прогнозную оценку
        return candidateSimilarities.entrySet()
                .stream()
                .map(entry -> {

                    List<EventSimilarity> topNeighbours =
                            entry.getValue()
                                    .stream()
                                    .sorted(
                                            Comparator.comparing(
                                                            EventSimilarity::getScore)
                                                    .reversed()
                                    )
                                    .limit(K_NEIGHBOURS)
                                    .toList();

                    double score = calculateScore(
                            topNeighbours,
                            seenEventIds,
                            actionWeights
                    );

                    return RecommendedEventProto.newBuilder()
                            .setEventId(entry.getKey())
                            .setScore(score)
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                        RecommendedEventProto::getScore)
                                .reversed()
                )
                .limit(limit)
                .toList();
    }

    /**
     * Вычисляет прогнозную оценку кандидата.
     *
     * Формула из ТЗ:
     *
     * Σ(weight × similarity) / Σ(similarity)
     */
    private double calculateScore(
            List<EventSimilarity> similarities,
            Set<Long> seenEventIds,
            Map<Long, Double> actionWeights
    ) {

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity similarity : similarities) {

            Long viewedEventId;

            if (seenEventIds.contains(similarity.getEventA())) {
                viewedEventId = similarity.getEventA();
            } else {
                viewedEventId = similarity.getEventB();
            }

            Double weight = actionWeights.get(viewedEventId);

            if (weight == null) {
                continue;
            }

            double similarityScore = similarity.getScore();

            weightedSum += weight * similarityScore;
            similaritySum += similarityScore;
        }

        return similaritySum == 0.0
                ? 0.0
                : weightedSum / similaritySum;
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
        List<EventWeightProjection> results =
                actionRepository.sumWeightsByEventIds(eventIds);

        return results.stream()
                .map(r -> RecommendedEventProto.newBuilder()
                        .setEventId(r.getEventId())
                        .setScore(r.getWeight())
                        .build())
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .toList();
    }
}