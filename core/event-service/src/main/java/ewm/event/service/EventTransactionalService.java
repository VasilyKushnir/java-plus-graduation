package ewm.event.service;

import ewm.event.mapper.EventMapper;
import ewm.event.model.Category;
import ewm.event.model.Event;
import ewm.event.repository.CategoryRepository;
import ewm.event.repository.DatabaseEventSearchRepository;
import ewm.event.repository.EventRepository;
import ewm.interaction.dto.event.NewEventDto;
import ewm.interaction.dto.event.UpdateEventAdminRequest;
import ewm.interaction.dto.event.UpdateEventUserRequest;
import ewm.interaction.dto.user.UserDto;
import ewm.interaction.enums.EventSort;
import ewm.interaction.enums.EventState;
import ewm.interaction.enums.EventStateActionAdmin;
import ewm.interaction.exception.BadRequestException;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventTransactionalService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final DatabaseEventSearchRepository databaseEventSearchRepository;

    @Transactional
    public Event createTransactional(UserDto user, NewEventDto eventDto) {
        isEventTimeValid(eventDto.getEventDate());

        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Event event = EventMapper.mapToEvent(user.getId(), eventDto, category.getId());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);

        return event;
    }

    @Transactional(readOnly = true)
    public Event getTransactional(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event not found");
        }

        return event;
    }

    @Transactional(readOnly = true)
    public List<Event> getTransactional(List<Long> users,
                                  List<EventState> states,
                                  List<Long> categories,
                                  LocalDateTime rangeStart,
                                  LocalDateTime rangeEnd,
                                  int from,
                                  int size) {
        Pageable page = PageRequest.of(from / size, size);
        return databaseEventSearchRepository.findForAdmin(users, states, categories, rangeStart, rangeEnd, page);
    }

    @Transactional(readOnly = true)
    public Event getPublicEventTransactional(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event is not published");
        }

        return event;
    }

    @Transactional(readOnly = true)
    public List<Event> getPublicEventsTransactional(String text,
                                               List<Long> categories,
                                               Boolean paid,
                                               LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd,
                                               Boolean onlyAvailable,
                                               EventSort sort,
                                               int from,
                                               int size) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("Range start must be before rangeEnd");
        }

        // Pageable: сортировка только по eventDate, не по views
        Pageable page;
        if (sort == EventSort.EVENT_DATE) {
            page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        } else {
            page = PageRequest.of(from / size, size);
        }

        return databaseEventSearchRepository.findPublicEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, page
        );
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsTransactional(Long userId, int from, int size) {
        Pageable page = PageRequest.of(from / size, size);
        return eventRepository.findByInitiatorId(userId, page);
    }

    @Transactional
    public Event updateTransactional(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (currentEvent.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event is already published");
        }

        if (!currentEvent.getInitiatorId().equals(userId)) {
            throw new BadRequestException("User not allowed to update event");
        }

        Event updatedEvent = EventMapper.updateEvent(currentEvent, updateEventUserRequest);

        if (updateEventUserRequest.hasCategory() &&
                !updatedEvent.getCategoryId().equals(updateEventUserRequest.getCategory())) {
            Category category = categoryRepository.findById(updateEventUserRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            updatedEvent.setCategoryId(category.getId());
        }

        isEventTimeValid(updatedEvent.getEventDate());
        return eventRepository.save(updatedEvent);
    }

    @Transactional
    public Event updateTransactional(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event currentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (currentEvent.getState().equals(EventState.PUBLISHED) &&
                updateEventAdminRequest.getEventDate() != null &&
                updateEventAdminRequest.getEventDate().isAfter(currentEvent.getPublishedOn().minusHours(1))) {
            throw new ConflictException("Invalid event time");
        }

        if (updateEventAdminRequest.getStateAction() != null) {
            if (currentEvent.getState().equals(EventState.PENDING)) {
                if (updateEventAdminRequest.getStateAction().equals(EventStateActionAdmin.PUBLISH_EVENT)) {
                    currentEvent.setState(EventState.PUBLISHED);
                    currentEvent.setPublishedOn(LocalDateTime.now());
                } else {
                    currentEvent.setState(EventState.CANCELED);
                }
            } else {
                throw new ConflictException("Invalid event state");
            }
        }

        Event updatedEvent = EventMapper.updateEvent(currentEvent, updateEventAdminRequest);

        if (updateEventAdminRequest.hasCategory() &&
                !updatedEvent.getCategoryId().equals(updateEventAdminRequest.getCategory())) {
            Category category = categoryRepository.findById(updateEventAdminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            updatedEvent.setCategoryId(category.getId());
        }

        return eventRepository.save(updatedEvent);
    }

    @Transactional(readOnly = true)
    public List<Event> getAllByIdTransactional(List<Long> ids) {
        return eventRepository.findAllByIdIn(ids);
    }

    @Transactional(readOnly = true)
    public Event findByIdTransactional(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private void isEventTimeValid(LocalDateTime eventTime) {
        if (eventTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Invalid event time");
        }
    }
}
