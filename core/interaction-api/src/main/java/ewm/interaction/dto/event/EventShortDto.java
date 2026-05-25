package ewm.interaction.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.user.UserShortDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private UserShortDto initiator;
    private Boolean paid;
    private Long views;
}