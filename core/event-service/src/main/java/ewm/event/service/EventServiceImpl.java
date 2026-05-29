package ewm.event.service;

import client.StatsClient;
import ewm.event.mapper.CategoryMapper;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Category;
import ewm.event.model.Event;
import ewm.event.repository.CategoryRepository;
import ewm.event.repository.DatabaseEventSearchRepository;
import ewm.event.repository.EventRepository;
import ewm.interaction.client.request.RequestClient;
import ewm.interaction.client.user.UserClient;
import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.event.*;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.dto.user.UserShortDto;
import ewm.interaction.enums.EventSort;
import ewm.interaction.enums.EventState;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

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
    private final StatsClient statsClient;

    private final UserClient userClient;
    private final RequestClient requestClient;

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
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        Event event = eventTxService.getPublicEventTransactional(eventId);
        registerHit(request);
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
            dtos.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }

        registerHit(request);

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

    private void registerHit(HttpServletRequest request) {
        EndpointHitDto endpointHitDto = new EndpointHitDto();
        endpointHitDto.setApp("main-service");
        endpointHitDto.setUri(request.getRequestURI());
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now());
        statsClient.hit(endpointHitDto);
    }

    private Map<Long, Integer> getEventsViews(List<Event> eventList) {
        if (eventList == null || eventList.isEmpty()) return Map.of();

        List<String> uris = eventList.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        LocalDateTime start = eventList.stream()
                .map(Event::getCreatedOn)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now().minusYears(1));

        LocalDateTime end = LocalDateTime.now();

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            Map<Long, Integer> map = new HashMap<>();
            for (ViewStatsDto s : stats) {
                String[] parts = s.getUri().split("/");
                if (parts.length >= 3) {
                    long eventId = Long.parseLong(parts[2]);
                    map.put(eventId, (int) s.getHits());
                }
            }
            return map;
        } catch (Exception ex) {
            // критично: не роняем эндпоинт
            return Map.of();
        }
    }

    private List<EventFullDto> mapToEventFullDto(List<Event> eventList) {
        if (eventList.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> views = getEventsViews(eventList);
        Map<Long, Integer> confirmed = getConfirmedRequests(eventList);
        Map<Long, UserShortDto> initiators = getInitiatorsDtoForEvents(eventList);
        Map<Long, CategoryDto> categories = getCategoriesDtoForEvents(eventList);

        return eventList.stream()
                .map(e -> EventMapper.mapToEventFullDto(
                        e,
                        views.getOrDefault(e.getId(), 0),
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


    @Override
    public List<EventShortDto> mapToEventShortDto(List<Event> eventList) {
        if (eventList.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> views = getEventsViews(eventList);
        Map<Long, Integer> confirmed = getConfirmedRequests(eventList);
        Map<Long, UserShortDto> initiators = getInitiatorsDtoForEvents(eventList);
        Map<Long, CategoryDto> categories = getCategoriesDtoForEvents(eventList);

        return eventList.stream()
                .map(e -> EventMapper.mapToEventShortDto(
                        e,
                        views.getOrDefault(e.getId(), 0),
                        confirmed.getOrDefault(e.getId(), 0),
                        initiators.get(e.getInitiatorId()),
                        categories.get(e.getCategoryId())
                ))
                .toList();
    }
}
