package ewm.request.repository;

import ewm.request.model.ParticipationRequest;
import ewm.request.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, Long eventId);

    @Query("""
           select r.eventId as eventId, count(r.id) as cnt
           from ParticipationRequest r
           where r.eventId in :eventIds and r.status = 'CONFIRMED'
           group by r.eventId
           """)
    List<EventConfirmedCount> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventConfirmedCount {
        Long getEventId();

        Integer getCnt();
    }

    long countByEventIdAndStatus(Long eventId, RequestStatus status);
}
