package cc.remer.photobook.adapter.web;

import cc.remer.photobook.adapter.security.UserPrincipal;
import cc.remer.photobook.adapter.web.api.UsersApi;
import cc.remer.photobook.adapter.web.mapper.UserMapper;
import cc.remer.photobook.adapter.web.model.CreateUserRequest;
import cc.remer.photobook.adapter.web.model.UpdateUserRequest;
import cc.remer.photobook.adapter.web.model.UserListResponse;
import cc.remer.photobook.adapter.web.model.UserResponse;
import cc.remer.photobook.domain.User;
import cc.remer.photobook.usecase.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.debug("Get current user request");

        UserPrincipal principal = getCurrentUserPrincipal();
        User user = userService.getCurrentUser(principal.getId());

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @Override
    public ResponseEntity<UserResponse> updateCurrentUser(UpdateUserRequest updateUserRequest) {
        log.debug("Update current user request");

        UserPrincipal principal = getCurrentUserPrincipal();

        User updatedUser = userService.updateCurrentUser(
                principal.getId(),
                updateUserRequest.getFirstName(),
                updateUserRequest.getLastName(),
                updateUserRequest.getPassword()
        );

        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @Override
    public ResponseEntity<UserListResponse> listUsers(Integer page, Integer size) {
        log.debug("List users request: page={}, size={}", page, size);

        Page<User> usersPage = userService.listUsers(page, size);

        List<UserResponse> users = usersPage.getContent().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        UserListResponse response = new UserListResponse()
                .content(users)
                .number(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
        log.debug("Create user request: {}", createUserRequest.getEmail());

        User user = userService.createUser(
                createUserRequest.getEmail(),
                createUserRequest.getPassword(),
                createUserRequest.getFirstName(),
                createUserRequest.getLastName(),
                createUserRequest.getRole() != null ? createUserRequest.getRole().getValue() : null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long userId) {
        log.debug("Delete user request: {}", userId);

        userService.deleteUserByMostSigBits(userId);

        return ResponseEntity.noContent().build();
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }
}
