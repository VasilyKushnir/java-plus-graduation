package ru.practicum.ewm.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.messages.ActionTypeProto;
import ru.practicum.ewm.stats.messages.UserActionProto;

import java.time.Instant;

@Component
public class UserActionMapper {
    public static UserActionAvro mapToAvro(UserActionProto action) {
        return UserActionAvro.newBuilder()
                .setUserId(action.getUserId())
                .setActionType(getType(action.getActionType()))
                .setEventId(action.getEventId())
                .setTimestamp(Instant.ofEpochSecond(
                        action.getTimestamp().getSeconds(),
                        action.getTimestamp().getNanos()))
                .build();
    }

    private static ActionTypeAvro getType(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized action type: " + actionTypeProto);
        };
    }
}