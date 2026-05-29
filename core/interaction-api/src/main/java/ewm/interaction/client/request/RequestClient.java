package ewm.interaction.client.request;

import ewm.interaction.client.configuration.FeignConfiguration;
import ewm.interaction.dto.request.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", configuration = FeignConfiguration.class)
public interface RequestClient {
    @GetMapping("/users/{userId}/events/{eventId}/request")
    ParticipationRequestDto getEventParticipationRequest(@PathVariable Long userId, @PathVariable Long eventId);

    @PostMapping("/events/requests/confirmed")
    Map<Long, Integer> getParticipationRequestsCountForEvents(@RequestBody List<Long> ids);
}
