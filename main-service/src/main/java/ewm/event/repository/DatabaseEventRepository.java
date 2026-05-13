package ewm.event.repository;

import ewm.event.model.Event;
import ewm.event.model.EventState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DatabaseEventRepository extends EventRepository, JpaRepository<Event, Long> {
    @Override
    @Query("""
            SELECT e
            FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart)
            AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)
            """)
    List<Event> findForAdmin(List<Long> users,
                             List<EventState> states,
                             List<Integer> categories,
                             LocalDateTime rangeStart,
                             LocalDateTime rangeEnd,
                             Pageable page);

    @Override
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%'))
                     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND (e.eventDate >= :rangeStart)
            AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)
            AND (:onlyAvailable IS NULL
                OR :onlyAvailable = FALSE
                OR e.participantLimit = 0
                OR e.confirmedRequests < e.participantLimit)
            """)
    List<Event> findPublicEvents(String text,
                                 List<Integer> categories,
                                 Boolean paid,
                                 LocalDateTime rangeStart,
                                 LocalDateTime rangeEnd,
                                 Boolean onlyAvailable,
                                 Pageable page);

    boolean existsByCategoryId(Long categoryId);
}
