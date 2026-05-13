package ewm.event.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.QEvent;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@AllArgsConstructor
public class DatabaseEventSearchRepository {
    private final JPAQueryFactory queryFactory;

    public List<Event> findForAdmin(List<Long> users,
                                    List<EventState> states,
                                    List<Long> categories,
                                    LocalDateTime rangeStart,
                                    LocalDateTime rangeEnd,
                                    Pageable pageable) {

        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (users != null && !users.isEmpty()) {
            builder.and(event.initiator.userId.in(users));
        }

        if (states != null && !states.isEmpty()) {
            builder.and(event.state.in(states));
        }

        if (categories != null && !categories.isEmpty()) {
            builder.and(event.category.id.in(categories));
        }

        if (rangeStart != null) {
            builder.and(event.eventDate.goe(rangeStart));
        }

        if (rangeEnd != null) {
            builder.and(event.eventDate.loe(rangeEnd));
        }

        return queryFactory
                .selectFrom(event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public List<Event> findPublicEvents(String text,
                                        List<Long> categories,
                                        Boolean paid,
                                        LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd,
                                        Boolean onlyAvailable,
                                        Pageable pageable) {

        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(event.state.eq(EventState.PUBLISHED));

        if (text != null && !text.isBlank()) {
            builder.and(
                    event.annotation.lower().contains(text.toLowerCase())
                            .or(event.description.lower().contains(text.toLowerCase()))
            );
        }

        if (categories != null && !categories.isEmpty()) {
            builder.and(event.category.id.in(categories));
        }

        if (paid != null) {
            builder.and(event.paid.eq(paid));
        }

        if (rangeStart != null) {
            builder.and(event.eventDate.goe(rangeStart));
        }

        if (rangeEnd != null) {
            builder.and(event.eventDate.loe(rangeEnd));
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            builder.and(
                    event.participantLimit.eq(0)
                            .or(event.confirmedRequests.lt(event.participantLimit))
            );
        }

        return queryFactory
                .selectFrom(event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
