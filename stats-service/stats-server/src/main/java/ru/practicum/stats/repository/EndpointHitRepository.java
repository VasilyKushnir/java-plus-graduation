package ru.practicum.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHitEntity, Long> {
    @Query("select new ru.practicum.ewm.stats.dto.ViewStatsDto(h.app, h.uri, count(h.id)) "
            + "from EndpointHitEntity h "
            + "where h.timestamp between :start and :end "
            + "and (:uris is null or h.uri in :uris) "
            + "group by h.app, h.uri "
            + "order by count(h.id) desc")
    List<ViewStatsDto> findStats(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("uris") List<String> uris);

    @Query("select new ru.practicum.ewm.stats.dto.ViewStatsDto(h.app, h.uri, count(distinct h.ip)) "
            + "from EndpointHitEntity h "
            + "where h.timestamp between :start and :end "
            + "and (:uris is null or h.uri in :uris) "
            + "group by h.app, h.uri "
            + "order by count(distinct h.ip) desc")
    List<ViewStatsDto> findStatsUnique(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("uris") List<String> uris);
}
