package ru.practicum.ewm.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для агрегации и расчета сходства между событиями
 * на основе действий пользователей и их весов.
 * <p>
 * Состояние (веса) сохраняется в полях класса и обновляется при каждом вызове,
 * что имитирует работу хранилища состояния (например, Redis).
 */
@Slf4j
@Component
public class AggregatorServiceImpl implements AggregatorService {

    // Взвешенные значения для различных типов действий, полученные из конфигурации.
    @Value("${application.action-weight.view}")
    private Double viewWeight;
    @Value("${application.action-weight.register}")
    private Double registerWeight;
    @Value("${application.action-weight.like}")
    private Double likeWeight;

    // Хранит вес действия пользователя для каждого события: EventId -> (UserId -> Weight)
    private Map<Long, Map<Long, Double>> eventUserWeight = new HashMap<>();
    // Хранит суммарный вес для каждого события: EventId -> TotalWeight
    private Map<Long, Double> eventWeightSum = new HashMap<>();
    // Хранит минимальную сумму весов для каждой пары событий: Min(EventA, EventB) -> (Max(EventA, EventB) -> MinWeightSum)
    // Использование min/max гарантирует, что пары всегда хранятся в упорядоченном виде.
    private Map<Long, Map<Long, Double>> minEventPairWeightSum = new HashMap<>();

    /**
     * Выполняет полный цикл агрегации:
     * 1. Рассчитывает изменение веса действия.
     * 2. Обновляет состояние весов (eventUserWeight).
     * 3. Обновляет общую сумму весов (eventWeightSum).
     * 4. Обновляет минимальные пары весов (minEventPairWeightSum).
     * 5. Рассчитывает и возвращает список структур сходства (EventSimilarityAvro).
     *
     * @param userActionAvro Предоставленное действие пользователя (содержит ID события, ID пользователя, тип и время).
     * @return Список структур сходства для всех сравниваемых пар событий.
     */
    @Override
    public List<EventSimilarityAvro> aggregateUserAction(SpecificRecordBase userActionAvro) {
        UserActionAvro action = (UserActionAvro) userActionAvro;
        List<EventSimilarityAvro> results = new ArrayList<>();

        // 1. Рассчитываем разницу веса. Это значение определяет, насколько сильно изменилось действие.
        Double weightDifference = calculateWeightDifference(action);
        if (weightDifference == null || weightDifference.equals(0.0)) {
            log.info("Изменение веса действия пользователя равно нулю или null. Возвращаем пустой список.");
            return results;
        }

        // 2. Обновляем глобальное состояние весов (обновляем eventUserWeight).
        updateEventUserWeight(action);

        // 3. Обновляем общую сумму весов события.
        updateEventWeightSum(action, weightDifference);

        // 4. Определяем все кандидаты для расчета сходства. Это все события,
        //    по которым пользователь совершил действие, кроме текущего события.
        List<Long> candidateEventIds = identifyCandidateEvents(action);
        if (candidateEventIds.isEmpty()) {
            log.info("Не найдено кандидатов событий для расчета сходства. Возвращаем пустой список.");
            return results;
        }

        // 5. Обновляем минимальные суммы весов для всех пар (текущее событие vs кандидаты).
        updateMinEventPairWeightSum(action, weightDifference, candidateEventIds);

        // 6. Рассчитываем и собираем итоговые значения сходства.
        log.info("Расчет сходства для события {} относительно событий: {}", action.getEventId(), candidateEventIds);
        for (Long candidateId : candidateEventIds) {
            // Стандартизация ключей для пары (A < B)
            Long eventA = Math.min(action.getEventId(), candidateId);
            Long eventB = Math.max(action.getEventId(), candidateId);

            // Получение компонентов для формулы сходства: MinSum / (sqrt(SumA) * sqrt(SumB))
            Double minSum = minEventPairWeightSum.getOrDefault(eventA, new HashMap<>()).get(eventB);
            Double sumA = eventWeightSum.getOrDefault(eventA, 0.0);
            Double sumB = eventWeightSum.getOrDefault(eventB, 0.0);

            if (minSum == null || sumA == 0.0 || sumB == 0.0) {
                log.warn("Не удалось рассчитать сходство для пары {} и {}: Отсутствуют необходимые компоненты.", eventA, eventB);
                continue;
            }

            // Основная формула сходства
            double similarity = minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));
            log.info("Сходство между событием {} и {} равно: {}", eventA, eventB, similarity);

            EventSimilarityAvro eventSimilarity = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity)
                    .setTimestamp(action.getTimestamp())
                    .build();
            results.add(eventSimilarity);
        }

        log.info("Завершено агрегирование. Общее количество расчетов: {}", results.size());
        return results;
    }

    /**
     * Определяет вес действия в зависимости от типа действия (VIEW, REGISTER, LIKE).
     */
    private Double getWeightValue(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }

    /**
     * Определяет список всех уникальных событий, которые могут быть сравнены с текущим событием.
     * Эти события должны иметь ненулевой вес для данного пользователя.
     *
     * @param action Текущее действие пользователя.
     * @return Список ID событий-кандидатов.
     */
    private List<Long> identifyCandidateEvents(UserActionAvro action) {
        long currentEventId = action.getEventId();
        long currentUserId = action.getUserId();

        return eventUserWeight.keySet().stream()
                .filter(eventId -> !eventId.equals(currentEventId))
                .filter(eventId -> {
                    Map<Long, Double> weightsForEvent = eventUserWeight.get(eventId);
                    Double weight = weightsForEvent != null ? weightsForEvent.get(currentUserId) : null;
                    // Фильтруем только те события, где пользователь действительно совершил действие (вес > 0).
                    return weight != null && weight > 0.0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Обновляет общую сумму весов для данного события, учитывая разницу, принесенную текущим действием.
     *
     * @param action           Текущее действие.
     * @param weightDifference Рассчитанная разница веса (новый вес - старый вес).
     */
    private void updateEventWeightSum(UserActionAvro action, Double weightDifference) {
        long eventId = action.getEventId();

        if (weightDifference == null || weightDifference.equals(0.0)) {
            log.info("Разница веса равна нулю. Обновление общей суммы весов для события {} пропущено.", eventId);
            return;
        }

        log.info("Обновление общей суммы весов для события: {}", eventId);

        eventWeightSum.compute(eventId, (key, existingSum) -> {
            if (existingSum == null) {
                // Если событие новое, устанавливаем сумму, равную текущему весу пользователя.
                // (В идеале, нам понадобился бы сам вес, а не только разница, но следуем текущей логике.)
                return eventUserWeight.getOrDefault(eventId, Map.of()).get(action.getUserId());
            }
            // Прибавляем разницу к существующей сумме.
            return existingSum + weightDifference;
        });
    }

    /**
     * Обновляет минимальную сумму весов между текущим событием и всеми кандидатами.
     * Эта сумма критична для расчета сходства. Обновление зависит от того,
     * как изменился вес текущего события.
     *
     * @param action            Текущее действие.
     * @param weightDifference  Изменение веса.
     * @param candidateEventIds События-кандидаты.
     */
    private void updateMinEventPairWeightSum(UserActionAvro action, Double weightDifference, List<Long> candidateEventIds) {
        long currentEventId = action.getEventId();
        long currentUserId = action.getUserId();

        // Текущий вес события после обновления (включает diff).
        Double currentEventWeight = eventUserWeight.getOrDefault(currentEventId, Map.of()).get(currentUserId);

        if (currentEventWeight == null || currentEventWeight.equals(0.0) || weightDifference == null || weightDifference.equals(0.0)) {
            log.info("Вес текущего события нулевой или разница нулевая. Обновление минимальных сумм пропущено.");
            return;
        }

        // Вес события *до* применения текущей разницы.
        Double previousEventWeight = currentEventWeight - weightDifference;
        log.info("Предыдущий вес для события {} был: {}", currentEventId, previousEventWeight);

        for (Long otherEventId : candidateEventIds) {
            // Вес кандидата (уже включен в расчет).
            Double otherEventWeight = eventUserWeight.getOrDefault(otherEventId, Map.of()).get(currentUserId);

            if (otherEventWeight == null || otherEventWeight.equals(0.0)) {
                continue;
            }

            // Стандартизация ключей для хранения в Map: A = min(E1, E2), B = max(E1, E2)
            long eventA = Math.min(currentEventId, otherEventId);
            long eventB = Math.max(currentEventId, otherEventId);

            // Получение карты пар весов для EventA.
            Map<Long, Double> pairWeights = minEventPairWeightSum.computeIfAbsent(eventA, k -> new HashMap<>());

            // Получение старого минимального значения. Если отсутствует, считаем 0.
            Double oldMinSum = pairWeights.getOrDefault(eventB, 0.0);
            Double newMinSum;

            if (oldMinSum == 0.0) {
                // Случай 1: Первая запись пары. Минимальная сумма равна минимуму двух весов.
                newMinSum = Math.min(currentEventWeight, otherEventWeight);
            } else {
                // Случай 2: Обновление существующей пары.

                if (currentEventWeight >= otherEventWeight) {
                    // Если текущий вес >= вес кандидата:
                    if (previousEventWeight >= otherEventWeight) {
                        // Если предыдущий вес тоже был >= веса кандидата, то минимальная сумма не изменилась.
                        newMinSum = oldMinSum;
                    } else {
                        // Предыдущий вес < вес кандидата (y). MinSum увеличивается на разницу (y - prev).
                        newMinSum = oldMinSum + (otherEventWeight - previousEventWeight);
                    }
                } else {
                    // Если текущий вес < вес кандидата. MinSum увеличивается на разницу веса текущего события.
                    newMinSum = oldMinSum + (currentEventWeight - previousEventWeight);
                }
            }

            pairWeights.put(eventB, newMinSum);
            log.info("Обновление минимальной суммы для пары {}-{} до: {}", eventA, eventB, newMinSum);
        }
    }


    /**
     * Рассчитывает разницу между новым весом и старым весом действия.
     * Эта разница передается в дальнейшие обновления.
     *
     * @param action Текущее действие пользователя.
     * @return Значение изменения веса.
     */
    private Double calculateWeightDifference(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        Double newWeight = getWeightValue(action.getActionType());

        log.info("Расчет изменения веса для события {} и пользователя {}. Новый вес: {}", eventId, userId, newWeight);

        Map<Long, Double> userWeights = eventUserWeight.get(eventId);

        if (userWeights == null || userWeights.isEmpty()) {
            // Случай 1: Первое действие на этом событии. Разница равна полному весу.
            return newWeight;
        }

        Double oldWeight = userWeights.get(userId);
        if (oldWeight == null || oldWeight.equals(0.0)) {
            // Случай 2: Первое действие пользователя на этом событии. Разница равна полному весу.
            return newWeight;
        }

        if (oldWeight >= newWeight) {
            // Случай 3: Вес не увеличился (или уменьшился, но логика считает, что он не должен).
            return 0.0;
        }

        // Случай 4: Нормальное увеличение.
        Double difference = newWeight - oldWeight;
        return difference;
    }

    /**
     * Обновляет специфический вес действия пользователя для данного события,
     * только если новый вес больше старого.
     *
     * @param action Текущее действие.
     */
    private void updateEventUserWeight(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        Double newWeight = getWeightValue(action.getActionType());

        log.info("Обновление веса пользователя {} в событии {} (Новый вес: {})", userId, eventId, newWeight);

        eventUserWeight.compute(eventId, (key, existingMap) -> {
            if (existingMap == null) {
                // Событие не существовало. Создаем карту.
                Map<Long, Double> newMap = new HashMap<>();
                newMap.put(userId, newWeight);
                return newMap;
            }

            Double oldWeight = existingMap.getOrDefault(userId, 0.0);

            if (oldWeight >= newWeight) {
                // Старый вес больше или равен новому. Не обновляем.
                return existingMap;
            } else {
                // Обновление произошло.
                existingMap.put(userId, newWeight);
                return existingMap;
            }
        });
    }
}
