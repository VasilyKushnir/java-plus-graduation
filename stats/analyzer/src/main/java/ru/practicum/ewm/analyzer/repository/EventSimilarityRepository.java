package ru.practicum.ewm.analyzer.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    Boolean existsByEventAAndEventB(Long eventA, Long eventB);

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    List<EventSimilarity> findAllByEventAInOrEventBIn(Set<Long> eventAIds, Set<Long> eventBIds, PageRequest pageRequest);

    @Query("""
                SELECT s
                FROM EventSimilarity s
                WHERE s.eventA = :id1 OR s.eventB = :id2
            """)
    List<EventSimilarity> findAllByEventAOrEventB(
            @Param("id1") Long id1,
            @Param("id2") Long id2,
            Pageable pageable
    );

}