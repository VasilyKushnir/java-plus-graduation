package ewm.compilation.mapper;

import ewm.compilation.dto.CompilationDto;
import ewm.compilation.model.Compilation;
import ewm.event.mapper.EventMapper;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation c) {
        return new CompilationDto(
                c.getId(),
                c.getTitle(),
                c.getPinned(),
                c.getEvents().stream()
                        .map(e -> EventMapper.mapToEventShortDto(e, 0L, 0L))
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

}
