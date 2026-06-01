package ewm.interaction.client.event;

import ewm.interaction.client.configuration.FeignConfiguration;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.UpdateEventAdminRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "event-service", configuration = FeignConfiguration.class)
public interface EventClient {
    @GetMapping("/admin/events/{eventId}")
    EventFullDto getEvent(@PathVariable Long eventId);

    @PatchMapping("/admin/events/{eventId}")
    EventFullDto updateEvent(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminRequest request);
}
