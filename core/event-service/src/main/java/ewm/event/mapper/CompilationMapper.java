package ewm.event.mapper;

import ewm.event.model.Compilation;
import ewm.interaction.dto.compilation.CompilationDto;
import ewm.interaction.dto.event.EventShortDto;

import java.util.Collection;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation c, Collection<EventShortDto> events) {
        return new CompilationDto(
                c.getId(),
                c.getTitle(),
                c.getPinned(),
                events
        );
    }

}
