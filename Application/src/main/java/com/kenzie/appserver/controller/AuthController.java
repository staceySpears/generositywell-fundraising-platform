package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.AuthResponse;
import com.kenzie.appserver.controller.model.LoginRequest;
import com.kenzie.appserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for authentication — currently handles password-based login. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code POST /auth/login} — authenticates a user and returns a signed JWT.
     * Always returns 401 with a generic message on failure to avoid leaking
     * whether the email address exists.
     *
     * @param request the login credentials (email + password)
     * @return 200 with the JWT and user ID, or 401 on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
