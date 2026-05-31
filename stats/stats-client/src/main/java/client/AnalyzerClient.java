package client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.messages.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.messages.RecommendedEventProto;
import ru.practicum.ewm.stats.messages.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.messages.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.services.RecommendationsControllerGrpc;

import java.util.*;

@Slf4j
@Component
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationsStub;

    public List<RecommendedEventProto> getRecommendations(Long userId, Integer maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        log.debug("Request recommendations: {}", request);

        try {
            List<RecommendedEventProto> result =
                    toList(recommendationsStub.getRecommendationsForUser(request));

            log.debug("Recommendations received: {} items", result.size());

            return result;
        } catch (Exception e) {
            log.error("Failed to get recommendations for userId={}", userId, e);
            throw e;
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId,
                                                        Long userId,
                                                        Integer maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        log.debug("Request similar events: {}", request);

        try {
            List<RecommendedEventProto> result =
                    toList(recommendationsStub.getSimilarEvents(request));

            log.debug("Similar events received: {} items", result.size());

            return result;
        } catch (Exception e) {
            log.error(
                    "Failed to get similar events. eventId={}, userId={}",
                    eventId,
                    userId,
                    e
            );
            throw e;
        }
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        log.debug("Request interactions count: {}", request);

        try {
            Map<Long, Double> result = new HashMap<>();

            recommendationsStub.getInteractionsCount(request)
                    .forEachRemaining(r -> result.put(r.getEventId(), r.getScore()));

            log.debug("Interactions count received: {} items", result.size());

            return result;
        } catch (Exception e) {
            log.error("Failed to get interactions count for events={}", eventIds, e);
            throw e;
        }
    }

    private static <T> List<T> toList(Iterator<T> iterator) {
        List<T> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}