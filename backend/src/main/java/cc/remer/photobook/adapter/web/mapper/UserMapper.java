package cc.remer.photobook.adapter.web.mapper;

import cc.remer.photobook.adapter.web.model.UserResponse;
import cc.remer.photobook.domain.User;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(UserResponse.RoleEnum.fromValue(user.getRole()));

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
        }

        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        return response;
    }
}
