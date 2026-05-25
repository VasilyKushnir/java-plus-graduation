package ewm.interaction.dto.compilation;

import ewm.interaction.dto.event.EventShortDto;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Collection;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompilationDto {
    private Long id;
    @Size(max = 50)
    private String title;
    private Boolean pinned;
    private Collection<EventShortDto> events;
}