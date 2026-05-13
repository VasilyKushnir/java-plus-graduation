package ewm.compilation.dto;

import ewm.event.dto.EventShortDto;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

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
    private Set<EventShortDto> events;
}