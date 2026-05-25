package ewm.user.service;

import ewm.interaction.dto.user.NewUserRequest;
import ewm.interaction.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto getUserById(Long userId);

    void delete(Long userId);
}
