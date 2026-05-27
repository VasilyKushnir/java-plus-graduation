package ewm.event.service;

import ewm.event.mapper.CompilationMapper;
import ewm.event.model.Compilation;
import ewm.event.model.Event;
import ewm.event.repository.CompilationRepository;
import ewm.event.repository.DatabaseEventRepository;
import ewm.interaction.dto.compilation.CompilationDto;
import ewm.interaction.dto.compilation.NewCompilationDto;
import ewm.interaction.dto.compilation.UpdateCompilationRequest;
import ewm.interaction.dto.event.EventShortDto;
import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final DatabaseEventRepository eventRepository;
    private final EventService eventService;

    private final TransactionTemplate transactionTemplate;

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        Compilation saved = transactionTemplate.execute(status -> {
            Compilation comp = new Compilation();

            comp.setTitle(dto.getTitle());
            comp.setPinned(dto.getPinned() != null ? dto.getPinned() : false);

            Set<Event> events = loadEvents(dto.getEvents());
            comp.setEvents(events);

            return compilationRepository.save(comp);
        });

        assert saved != null;
        return CompilationMapper.toDto(saved, eventService.mapToEventShortDto(new ArrayList<>(saved.getEvents())));
    }


    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation saved = transactionTemplate.execute(status -> {
            Compilation comp = compilationRepository.findById(compId)
                    .orElseThrow(() -> new NotFoundException("Compilation not found: " + compId));

            if (dto.getTitle() != null) {
                comp.setTitle(dto.getTitle());
            }
            if (dto.getPinned() != null) {
                comp.setPinned(dto.getPinned());
            }
            if (dto.getEvents() != null) {
                comp.setEvents(loadEvents(dto.getEvents()));
            }

            return compilationRepository.save(comp);
        });

        assert saved != null;
        return CompilationMapper.toDto(saved, eventService.mapToEventShortDto(new ArrayList<>(saved.getEvents())));
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation not found: " + compId);
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> findAll(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> comps = (pinned == null)
                ? compilationRepository.findAll(pageable).getContent()
                : compilationRepository.findAllByPinned(pinned, pageable);

        return mapToDtos(comps);
    }

    @Override
    public CompilationDto findById(Long compId) {
        Compilation comp = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found: " + compId));
        return CompilationMapper.toDto(comp, eventService.mapToEventShortDto(new ArrayList<>(comp.getEvents())));
    }

    private List<CompilationDto> mapToDtos(List<Compilation> comps) {
        Set<Event> events = new HashSet<>();
        for (Compilation comp : comps) {
            events.addAll(comp.getEvents());
        }
        Map<Long, EventShortDto> eventDtos = eventService.mapToEventShortDto(new ArrayList<>(events))
                .stream().collect(Collectors.toMap(EventShortDto::getId, e -> e));

        return comps.stream().map(
                c -> CompilationMapper.toDto(c,
                        c.getEvents().stream().map(e -> eventDtos.get(e.getId())).toList())).toList();
    }

    private Set<Event> loadEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Some events not found");
        }

        return new HashSet<>(events);
    }
}