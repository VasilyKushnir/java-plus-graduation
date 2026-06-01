package ru.practicum.ewm.analyzer.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.analyzer.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<UserAction> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT a.eventId FROM UserAction a WHERE a.userId = :userId")
    Set<Long> findAllEventIdsByUserId(Long userId);

    @Query("""
            SELECT a.eventId as eventId,
                   SUM(a.weight) as weight
            FROM UserAction a
            WHERE a.eventId IN :eventIds
            GROUP BY a.eventId
            """)
    List<EventWeightProjection> sumWeightsByEventIds(List<Long> eventIds);

    List<UserAction> findAllByUserId(Long userId, PageRequest pageRequest);
}
