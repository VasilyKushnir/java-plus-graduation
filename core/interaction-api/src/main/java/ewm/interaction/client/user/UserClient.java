package ewm.interaction.client.user;

import ewm.interaction.client.configuration.FeignConfiguration;
import ewm.interaction.dto.user.UserDto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient(name = "user-service", configuration = FeignConfiguration.class)
public interface UserClient {
    @GetMapping("/admin/users")
    Collection<UserDto> getUsers(@RequestParam Collection<Long> ids,
                                 @RequestParam @PositiveOrZero Integer from,
                                 @RequestParam @Positive Integer size);

    @GetMapping("/admin/users/{userId}")
    UserDto getUser(@PathVariable Long userId);
}