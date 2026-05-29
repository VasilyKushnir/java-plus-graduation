package ewm.request.mapper;

import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.request.model.ParticipationRequest;

public final class ParticipationRequestMapper {

    private ParticipationRequestMapper() {
    }

    public static ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
                .id(r.getId())
                .created(r.getCreated())
                .event(r.getEventId())
                .requester(r.getRequesterId())
                .status(r.getStatus().name())
                .build();
    }
}
