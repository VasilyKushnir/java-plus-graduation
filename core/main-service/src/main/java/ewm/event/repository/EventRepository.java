package ewm.event.repository;

import ewm.event.model.Event;
import ewm.event.model.EventState;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);

    List<Event> findByInitiatorUserId(Long initiatorId, Pageable page);

    List<Event> findForAdmin(List<Long> users,
                             List<EventState> states,
                             List<Integer> categories,
                             LocalDateTime rangeStart,
                             LocalDateTime rangeEnd,
                             Pageable page);

    List<Event> findPublicEvents(String text,
                                 List<Integer> categories,
                                 Boolean paid,
                                 LocalDateTime rangeStart,
                                 LocalDateTime rangeEnd,
                                 Boolean onlyAvailable,
                                 Pageable page);

    Optional<Event> findById(Long eventId);

    Optional<Event> findByIdAndState(Long eventId, EventState eventState);
}
