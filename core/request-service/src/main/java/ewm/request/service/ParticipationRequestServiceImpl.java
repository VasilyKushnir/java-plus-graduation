package ewm.request.service;

import client.CollectorClient;
import ewm.interaction.client.event.EventClient;
import ewm.interaction.client.user.UserClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.request.EventRequestStatusUpdateRequest;
import ewm.interaction.dto.request.EventRequestStatusUpdateResult;
import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.enums.EventState;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.NotFoundException;
import ewm.request.mapper.ParticipationRequestMapper;
import ewm.request.model.ParticipationRequest;
import ewm.request.model.RequestStatus;
import ewm.request.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepo;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CollectorClient collectorClient;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        UserDto user;
        EventFullDto event;
        try {
            user = userClient.getUser(userId);
        } catch (NotFoundException e) {
            throw new ConflictException("User not found");
        }

        try {
            event = eventClient.getEvent(eventId);
        } catch (NotFoundException e) {
            throw new ConflictException("Event not found");
        }

        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Initiator cannot request own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            throw new ConflictException("Event not published");
        }
        if (requestRepo.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Duplicate request");
        }

        long limit = event.getParticipantLimit();
        long confirmed = event.getConfirmedRequests();

        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("Participant limit reached");
        }

        ParticipationRequest req = new ParticipationRequest();
        req.setRequesterId(user.getId());
        req.setEventId(event.getId());

        RequestStatus status;
        if (event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration())) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }
        req.setStatus(status);

        // если автоподтверждение и лимит > 0 — повторная защита от гонки
        if (status == RequestStatus.CONFIRMED && limit > 0) {
            long confirmedAfter = requestRepo.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedAfter >= limit) {
                throw new ConflictException("Participant limit reached");
            }
        }

        ParticipationRequest saved = requestRepo.save(req);

        collectorClient.sendRegistration(userId, eventId);

        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest req = requestRepo.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request not found: " + requestId));

        req.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        ensureUserExists(userId);
        return requestRepo.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        EventFullDto event = eventClient.getEvent(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only initiator can view event requests");
        }
        return requestRepo.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(
            Long userId,
            Long eventId,
            List<Long> requestIds,
            EventRequestStatusUpdateRequest.RequestUpdateStatus status
    ) {
        EventFullDto event = eventClient.getEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only initiator can update requests");
        }

        List<ParticipationRequest> requests =
                requestRepo.findAllByIdInAndEventId(requestIds, eventId);

        if (requests.size() != new HashSet<>(requestIds).size()) {
            throw new NotFoundException("Some requests not found for event");
        }

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Only PENDING requests can be updated");
            }
        }

        List<ParticipationRequest> confirmedOut = new ArrayList<>();
        List<ParticipationRequest> rejectedOut = new ArrayList<>();

        if (status == EventRequestStatusUpdateRequest.RequestUpdateStatus.REJECTED) {
            for (ParticipationRequest r : requests) {
                r.setStatus(RequestStatus.REJECTED);
                rejectedOut.add(r);
            }
            return toResult(confirmedOut, rejectedOut);
        }

        // status == CONFIRMED
        long limit = event.getParticipantLimit();
        long confirmed = event.getConfirmedRequests();

        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("Participant limit reached");
        }

        long slots = (limit == 0) ? Long.MAX_VALUE : (limit - confirmed);

        for (ParticipationRequest r : requests) {
            if (slots > 0) {
                r.setStatus(RequestStatus.CONFIRMED);
                confirmedOut.add(r);
                slots--;
                confirmed++;
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejectedOut.add(r);
            }
        }

        return toResult(confirmedOut, rejectedOut);
    }

    @Override
    public Map<Long, Integer> getConfirmedRequestsCount(List<Long> eventIds) {
        List<ParticipationRequestRepository.EventConfirmedCount> count = requestRepo.countConfirmedByEventIds(eventIds);
        return count.stream().collect(Collectors.toMap(
                ParticipationRequestRepository.EventConfirmedCount::getEventId,
                ParticipationRequestRepository.EventConfirmedCount::getCnt
        ));
    }

    private EventRequestStatusUpdateResult toResult(List<ParticipationRequest> confirmed,
                                                    List<ParticipationRequest> rejected) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(ParticipationRequestMapper::toDto).toList())
                .rejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).toList())
                .build();
    }

    private void ensureUserExists(Long userId) {
        if (userClient.getUser(userId) == null) {
            throw new NotFoundException("User not found: " + userId);
        }
    }
}