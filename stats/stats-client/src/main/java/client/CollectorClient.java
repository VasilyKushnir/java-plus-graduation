package client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.messages.ActionTypeProto;
import ru.practicum.ewm.stats.messages.UserActionProto;
import ru.practicum.ewm.stats.services.UserActionControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public void sendRegistration(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private void sendAction(
            Long userId,
            Long eventId,
            ActionTypeProto actionType
    ) {
        Instant now = Instant.now();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(
                        Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano())
                                .build()
                )
                .build();

        log.info(
                "Send action={}, userId={}, eventId={}",
                actionType,
                userId,
                eventId
        );

        collectorStub.collectUserAction(request);
    }
}