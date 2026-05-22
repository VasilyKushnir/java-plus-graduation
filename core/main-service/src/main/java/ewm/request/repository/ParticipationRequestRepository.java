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

    boolean existsByEventIdAndRequesterUserId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequesterUserId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterUserId(Long id, Long requesterId);

    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, Long eventId);

    @Query("""
           select r.event.id as eventId, count(r.id) as cnt
           from ParticipationRequest r
           where r.event.id in :eventIds and r.status = 'CONFIRMED'
           group by r.event.id
           """)
    List<EventConfirmedCount> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventConfirmedCount {
        Long getEventId();

        Long getCnt();
    }

    long countByEventIdAndStatus(Long eventId, RequestStatus status);
}
