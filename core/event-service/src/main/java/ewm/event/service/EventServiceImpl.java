package ewm.event.service;

import client.AnalyzerClient;
import client.CollectorClient;
import ewm.event.mapper.CategoryMapper;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Category;
import ewm.event.model.Event;
import ewm.event.repository.CategoryRepository;
import ewm.interaction.client.request.RequestClient;
import ewm.interaction.client.user.UserClient;
import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.event.*;
import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.enums.EventSort;
import ewm.interaction.enums.EventState;
import ewm.interaction.enums.RequestStatus;
import ewm.interaction.exception.BadRequestException;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.messages.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final CategoryRepository categoryRepository;
    private final AnalyzerClient analyzerClient;

    private final UserClient userClient;
    private final RequestClient requestClient;
    private final CollectorClient collectorClient;

    private final EventTransactionalService eventTxService;

    @Override
    public EventFullDto create(Long userId, NewEventDto eventDto) {
        UserDto user = userClient.getUser(userId);
        Event event = eventTxService.createTransactional(user, eventDto);
        return this.mapToEventFullDto(List.of(event)).getFirst();
    }

    @Override
    public EventFullDto get(Long userId, Long eventId) {
        Event event = eventTxService.getTransactional(userId, eventId);
        return this.mapToEventFullDto(List.of(event)).getFirst();
    }

    @Override
    public List<EventFullDto> get(List<Long> users,
                                  List<EventState> states,
                                  List<Long> categories,
                                  LocalDateTime rangeStart,
                                  LocalDateTime rangeEnd,
                                  int from,
                                  int size) {
        List<Event> eventList = eventTxService
                .getTransactional(users, states, categories, rangeStart, rangeEnd, from, size);

        return this.mapToEventFullDto(eventList);
    }

    @Override
    public EventFullDto getPublicEvent(Long userId, Long eventId, HttpServletRequest request) {
        Event event = eventTxService.getPublicEventTransactional(eventId);

        collectorClient.sendView(userId, eventId);
        return this.mapToEventFullDto(List.of(event)).getFirst();
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        userClient.getUser(userId);
        List<Event> eventList = eventTxService.getEventsTransactional(userId, from, size);
        return this.mapToEventShortDto(eventList);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text,
                                               List<Long> categories,
                                               Boolean paid,
                                               LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd,
                                               Boolean onlyAvailable,
                                               EventSort sort,
                                               int from,
                                               int size,
                                               HttpServletRequest request) {

        List<Event> eventList = eventTxService.getPublicEventsTransactional(text,
                categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        List<EventShortDto> dtos = mapToEventShortDto(eventList);
        if (sort == EventSort.VIEWS) {
            dtos.sort(Comparator.comparingDouble(EventShortDto::getRating).reversed());
        }

        return dtos;
    }


    @Override
    public EventFullDto update(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event updatedEvent = eventTxService.updateTransactional(userId, eventId, updateEventUserRequest);
        List<Event> eventList = List.of(updatedEvent);
        return this.mapToEventFullDto(eventList).getFirst();
    }

    @Override
    public EventFullDto update(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event updatedEvent = eventTxService.updateTransactional(eventId, updateEventAdminRequest);
        List<Event> eventList = List.of(updatedEvent);
        return this.mapToEventFullDto(eventList).getFirst();
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        try {
            ParticipationRequestDto request = requestClient.getEventParticipationRequest(userId, eventId);
            if (!RequestStatus.CONFIRMED.name().equals(request.getStatus())) {
                throw new BadRequestException("User can only like attended events.");
            }

            collectorClient.sendLike(userId, eventId);
        } catch (FeignException e) {
            throw new BadRequestException("Error calling request-service.");
        }
    }

    @Override
    public List<EventFullDto> getRecommendations(Long userId, Integer maxResults) {
        List<Long> ids = analyzerClient.getRecommendations(userId, maxResults)
                .stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        List<Event> events = eventTxService.getAllByIdTransactional(ids);
        List<EventFullDto> dtos = this.mapToEventFullDto(events);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }

        dtos.sort(Comparator.comparingInt(dto -> order.getOrDefault(dto.getId(), Integer.MAX_VALUE)));
        return dtos;
    }

    private List<EventFullDto> mapToEventFullDto(List<Event> eventList) {
        if (eventList.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> confirmed = getConfirmedRequests(eventList);
        Map<Long, UserShortDto> initiators = getInitiatorsDtoForEvents(eventList);
        Map<Long, CategoryDto> categories = getCategoriesDtoForEvents(eventList);
        Map<Long, Double> ratings = getRatingsForEvents(eventList);

        return eventList.stream()
                .map(e -> EventMapper.mapToEventFullDto(
                        e,
                        ratings.getOrDefault(e.getId(), 0.0),
                        confirmed.getOrDefault(e.getId(), 0),
                        initiators.get(e.getInitiatorId()),
                        categories.get(e.getCategoryId())
                ))
                .toList();
    }

    private Map<Long, Integer> getConfirmedRequests(List<Event> eventList) {
        List<Long> ids = eventList.stream().map(Event::getId).toList();
        if (ids.isEmpty()) return Map.of();

        try {
            return requestClient.getParticipationRequestsCountForEvents(ids);
        } catch (Exception e) {
            log.error("Ошибка при получении количества одобренных заявок для событий: {}; {}", ids, e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, UserShortDto> getInitiatorsDtoForEvents(List<Event> eventList) {
        List<Long> userIds = eventList.stream().map(Event::getInitiatorId).toList();
        return userClient.getUsers(userIds, 0, userIds.size())
                .stream()
                .map(u -> UserShortDto.builder()
                        .name(u.getName())
                        .id(u.getId()).build())
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));
    }

    private Map<Long, CategoryDto> getCategoriesDtoForEvents(List<Event> eventList) {
        List<Long> categoryIds = eventList.stream().map(Event::getCategoryId).toList();
        List<Category> categories = categoryRepository.findByIdIn(categoryIds);
        return categories.stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toMap(CategoryDto::getId, c -> c));
    }

    private Map<Long, Double> getRatingsForEvents(List<Event> eventList) {
        List<Long> eventIds = eventList.stream().map(Event::getId).toList();
        return analyzerClient.getInteractionsCount(eventIds);
    }


    @Override
    public List<EventShortDto> mapToEventShortDto(List<Event> eventList) {
        if (eventList.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> confirmed = getConfirmedRequests(eventList);
        Map<Long, UserShortDto> initiators = getInitiatorsDtoForEvents(eventList);
        Map<Long, CategoryDto> categories = getCategoriesDtoForEvents(eventList);
        Map<Long, Double> ratings = getRatingsForEvents(eventList);

        return eventList.stream()
                .map(e -> EventMapper.mapToEventShortDto(
                        e,
                        ratings.getOrDefault(e.getId(), 0.0),
                        confirmed.getOrDefault(e.getId(), 0),
                        initiators.get(e.getInitiatorId()),
                        categories.get(e.getCategoryId())
                ))
                .toList();
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        return this.mapToEventFullDto(List.of(eventTxService.findByIdTransactional(eventId))).getFirst();
    }
}
