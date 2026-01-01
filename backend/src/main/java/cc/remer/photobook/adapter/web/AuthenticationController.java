package cc.remer.photobook.adapter.web;

import cc.remer.photobook.adapter.web.api.AuthenticationApi;
import cc.remer.photobook.adapter.web.mapper.UserMapper;
import cc.remer.photobook.adapter.web.model.AuthResponse;
import cc.remer.photobook.adapter.web.model.LoginRequest;
import cc.remer.photobook.adapter.web.model.LogoutRequest;
import cc.remer.photobook.adapter.web.model.RefreshTokenRequest;
import cc.remer.photobook.config.JwtProperties;
import cc.remer.photobook.usecase.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationApi {

    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        log.debug("Login request for email: {}", loginRequest.getEmail());

        AuthenticationService.AuthResult result = authenticationService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        AuthResponse response = new AuthResponse()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .user(userMapper.toResponse(result.getUser()));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.debug("Refresh token request");

        AuthenticationService.AuthResult result = authenticationService.refresh(
                refreshTokenRequest.getRefreshToken()
        );

        AuthResponse response = new AuthResponse()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .user(userMapper.toResponse(result.getUser()));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> logout(LogoutRequest logoutRequest) {
        log.debug("Logout request");

        authenticationService.logout(logoutRequest.getRefreshToken());

        return ResponseEntity.noContent().build();
    }
}
