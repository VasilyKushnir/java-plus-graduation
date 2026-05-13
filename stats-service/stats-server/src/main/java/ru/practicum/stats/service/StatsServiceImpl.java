package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.mapper.EndpointHitMapper;
import ru.practicum.stats.model.EndpointHitEntity;
import ru.practicum.stats.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final EndpointHitRepository hitRepository;

    @Override
    @Transactional(readOnly = false)
    public void saveHit(EndpointHitDto hit) {
        EndpointHitEntity entity = EndpointHitMapper.toEntity(hit);
        log.info("save endpoint hit {}", entity);
        hitRepository.save(entity);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (end.isBefore(start)) {
            throw new BadRequestException("Range start date is before end date");
        }
        List<String> filterUris = (uris == null || uris.isEmpty()) ? null : uris;
        if (unique) {
            return hitRepository.findStatsUnique(start, end, filterUris);
        }
        return hitRepository.findStats(start, end, filterUris);
    }
}
